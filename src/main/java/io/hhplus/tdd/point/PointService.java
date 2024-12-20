package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.excpetion.UserNotFoundException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    PointService (PointHistoryTable pointHistoryTable, UserPointTable userPointTable){
        this.pointHistoryTable = pointHistoryTable;
        this.userPointTable = userPointTable;
    }

    // 각 ID별 Lock을 관리하는 ConcurrentHashMap
    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();

    private Lock getLock(long id) {
        // ID에 해당하는 Lock이 없으면 새로 생성해서 추가
        return lockMap.computeIfAbsent(id, k -> new ReentrantLock());
    }

     /**
     *  User의 포인트를 조회한다.
     */
    public UserPoint getUserPoint(long id) throws RuntimeException {
        if(pointHistoryTable.selectAllByUserId(id).isEmpty()){
            throw new UserNotFoundException("사용자 정보가 없습니다.");
        }
        return userPointTable.selectById(id);
    }

    /**
     *  User의 포인트를 충전한다.
     */
    UserPoint chargePoint(long id, long amount) throws RuntimeException {
        Lock lock = getLock(id);
        lock.lock();
        try {
            if (amount < 1) {
                throw new IllegalArgumentException("1포인트 미만은 충전할 수 없습니다.");
            }
            if (amount > 100000) {
                throw new IllegalArgumentException("한번에 10만 포인트를 초과해서 충전할 수 없습니다.");
            }
            UserPoint origin = userPointTable.selectById(id);
            if (origin.point() + amount > 100000000) {
                throw new IllegalArgumentException("1억 포인트를 초과해서 보유할 수 없습니다.");
            }
            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return userPointTable.insertOrUpdate(id, origin.point() + amount);
        } finally {
            lock.unlock();
        }
    }


    /**
     *  User의 포인트를 사용한다.
     */
    UserPoint usePoint(long id, long amount) throws RuntimeException {
        Lock lock = getLock(id);
        lock.lock();
        try {
            if (pointHistoryTable.selectAllByUserId(id).isEmpty()) {
                throw new UserNotFoundException("사용자 정보가 없습니다.");
            }
            if (amount < 1) {
                throw new IllegalArgumentException("최소 1포인트부터 사용할 수 있습니다.");
            }
            if (amount > 1000000) {
                throw new IllegalArgumentException("한번에 최대 100만 포인트까지 사용할 수 있습니다.");
            }
            UserPoint origin = userPointTable.selectById(id);
            if (origin.point() < amount) {
                throw new IllegalArgumentException("보유한 포인트를 초과해서 사용할 수 없습니다.");
            }
            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
            return userPointTable.insertOrUpdate(id, origin.point() - amount);
        } finally {
            lock.unlock();
        }
    }

    /**
     *  User의 포인트 충전/사용 내역을 조회한다.
     */
    List<PointHistory> getPointHistory(long id) throws RuntimeException {
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(id);
        if (histories.isEmpty()) {
            throw new UserNotFoundException("사용자 정보가 없습니다.");
        }
        return histories;
    }

}
