package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PointServiceConcurrencyTest {
    private static final Logger logger = LoggerFactory.getLogger(PointServiceConcurrencyTest.class);
    private final PointHistoryTable pointHistoryTable = new PointHistoryTable();
    private final UserPointTable userPointTable = new UserPointTable();
    private final PointService pointService = new PointService(pointHistoryTable, userPointTable);

    private final BlockingQueue<Runnable> taskQueue1 = new LinkedBlockingQueue<>();
    private final BlockingQueue<Runnable> taskQueue2 = new LinkedBlockingQueue<>();
    private final BlockingQueue<Runnable> taskQueue3 = new LinkedBlockingQueue<>();

    @Test
    void testConcurrentUserRequestsWithMultipleQueues() throws InterruptedException {

        ExecutorService executorService1 = Executors.newFixedThreadPool(3); // 첫 번째 큐를 처리할 스레드 풀
        ExecutorService executorService2 = Executors.newFixedThreadPool(5); // 두 번째 큐를 처리할 스레드 풀
        ExecutorService executorService3 = Executors.newFixedThreadPool(4); // 세 번째 큐를 처리할 스레드 풀

        // 예상 PointHistory
        List<PointHistory> user1History = new ArrayList<>();
        user1History.add(new PointHistory(1,1, 100,TransactionType.CHARGE,1));
        user1History.add(new PointHistory(1,1, 100,TransactionType.CHARGE,1));
        user1History.add(new PointHistory(1,1, 100,TransactionType.CHARGE,1));

        List<PointHistory> user2History = new ArrayList<>();
        user2History.add(new PointHistory(1,2, 100,TransactionType.CHARGE,1));
        user2History.add(new PointHistory(1,2, 50,TransactionType.USE,1));
        user2History.add(new PointHistory(1,2, 50,TransactionType.USE,1));
        user2History.add(new PointHistory(1,2, 100,TransactionType.CHARGE,1));
        user2History.add(new PointHistory(1,2, 50,TransactionType.USE,1));

        List<PointHistory> user3History = new ArrayList<>();
        user3History.add(new PointHistory(1,3, 100,TransactionType.CHARGE,1));
        user3History.add(new PointHistory(1,3, 100,TransactionType.CHARGE,1));
        user3History.add(new PointHistory(1,3, 50,TransactionType.USE,1));
        user3History.add(new PointHistory(1,3, 50,TransactionType.USE,1));

        // 첫 번째 큐에 작업 추가
        taskQueue1.add(() -> pointService.chargePoint(1L, 100));
        taskQueue1.add(() -> pointService.chargePoint(1L, 100));
        taskQueue1.add(() -> pointService.chargePoint(1L, 100));

        // 두 번째 큐에 작업 추가
        taskQueue2.add(() -> pointService.chargePoint(2L, 100));
        taskQueue2.add(() -> pointService.usePoint(2L, 50));
        taskQueue2.add(() -> pointService.usePoint(2L, 50));
        taskQueue2.add(() -> pointService.chargePoint(2L, 100));
        taskQueue2.add(() -> pointService.usePoint(2L, 50));

        // 세 번째 큐에 작업 추가
        taskQueue3.add(() -> pointService.chargePoint(3L, 100));
        taskQueue3.add(() -> pointService.chargePoint(3L, 100));
        taskQueue3.add(() -> pointService.usePoint(3L, 50));
        taskQueue3.add(() -> pointService.usePoint(3L, 50));

        // 각 큐를 병렬로 처리하기 위해 ExecutorService에 작업 제출
        submitQueueTasks(executorService1, taskQueue1);
        submitQueueTasks(executorService2, taskQueue2);
        submitQueueTasks(executorService3, taskQueue3);

        // 작업 완료 대기
        executorService1.shutdown();
        executorService2.shutdown();
        executorService3.shutdown();

        executorService1.awaitTermination(1, TimeUnit.MINUTES);
        executorService2.awaitTermination(1, TimeUnit.MINUTES);
        executorService3.awaitTermination(1, TimeUnit.MINUTES);

        // 결과 검증
        assertThat(userPointTable.selectById(1L).point()).isEqualTo(300);
        assertThat(userPointTable.selectById(2L).point()).isEqualTo(50);
        assertThat(userPointTable.selectById(3L).point()).isEqualTo(100);

        assertTrue(matchesHistory(user1History, pointHistoryTable.selectAllByUserId(1L)));
        assertTrue(matchesHistory(user2History, pointHistoryTable.selectAllByUserId(2L)));
        assertTrue(matchesHistory(user3History, pointHistoryTable.selectAllByUserId(3L)));

    }

    // 큐에 작업 추가
    private void submitQueueTasks(ExecutorService executorService, BlockingQueue<Runnable> taskQueue) {
        executorService.submit(() -> {
            while (!taskQueue.isEmpty()) {
                try {
                    Runnable task = taskQueue.poll(); // 큐에서 작업을 꺼냄
                    if (task != null) {
                        task.run();
                    }
                } catch (Exception e) {
                    logger.error("Error processing task", e);
                }
            }
        });
    }

    // PointHistory와 비교
    private boolean matchesHistory(List<PointHistory> expected, List<PointHistory> actual) {
        // 크기가 같아야 함
        if (expected.size() != actual.size()) {
            return false;
        }

        // 각 PointHistory 의 amount 와 TransactionType 이 같은지 비교
        for (int i = 0; i < expected.size(); i++) {
            PointHistory expectedHistory = expected.get(i);
            PointHistory actualHistory = actual.get(i);

            if (expectedHistory.amount() != actualHistory.amount() ||
                    expectedHistory.type() != actualHistory.type()) {
                return false;
            }
        }

        return true;
    }

}