package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.excpetion.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UsePointServiceTest {
    @Mock
    private PointService pointService;
    @Mock
    private PointHistoryTable pointHistoryTable;
    @Mock
    private UserPointTable userPointTable;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

    @Test
    @DisplayName("포인트 충전 내역이 없는 사용자는 포인트를 사용할 수 없다.")
    void if_history_not_exist_fail(){
        // Given
        long userId = 1L;
        long amount = 100;
        long currentTime = System.currentTimeMillis();
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.emptyList());

        // When
        Exception exception = assertThrows(UserNotFoundException.class,
                ()-> pointService.usePoint(userId, amount, currentTime));

        // Then
        assertEquals("사용자 정보가 없습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }


    @Test
    @DisplayName("최소 1포인트부터 사용할 수 있다.")
    void if_less_one_point_use_fail(){
        // Given
        long userId = 1L;
        long amount = 0;
        long currentTime = System.currentTimeMillis();
        PointHistory pointHistory = new PointHistory(0, userId, 0, TransactionType.CHARGE, 0);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(pointHistory));
        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.usePoint(userId, amount, currentTime));

        // Then
        assertEquals("최소 1포인트부터 사용할 수 있습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("한번에 1_000_000 포인트 이상 사용할 수 없다.")
    void if_over_one_million_use_fail(){
        // Given
        long userId = 1L;
        long amount = 10000000;
        long currentTime = System.currentTimeMillis();
        PointHistory pointHistory = new PointHistory(0, userId, 0, TransactionType.CHARGE, 0);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(pointHistory));
        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.usePoint(userId, amount, currentTime));

        // Then
        assertEquals("한번에 최대 100만 포인트까지 사용할 수 있습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("보유한 포인트 이상은 사용할 수 없다.")
    void if_over_balance_use_fail(){
        // Given
        long userId = 1L;
        long amount = 1000;
        long currentTime = System.currentTimeMillis();
        PointHistory pointHistory = new PointHistory(0, userId, 100, TransactionType.CHARGE, 0);
        UserPoint origin = new UserPoint(userId, 100, System.currentTimeMillis());
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(pointHistory));
        when(userPointTable.selectById(userId)).thenReturn(origin);

        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                ()-> pointService.usePoint(userId, amount, currentTime));

        // Then
        assertEquals("보유한 포인트를 초과해서 사용할 수 없습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("성공케이스")
    void success(){
        // Given
        long userId = 1L;
        long amount = 100;
        long currentTime = System.currentTimeMillis();
        PointHistory pointHistory = new PointHistory(0, userId, 100, TransactionType.CHARGE, 0);
        UserPoint origin = new UserPoint(userId, 300, System.currentTimeMillis());
        UserPoint used = new UserPoint(userId, origin.point()-amount, currentTime);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(pointHistory));
        when(userPointTable.selectById(userId)).thenReturn(origin);
        when(userPointTable.insertOrUpdate(userId, origin.point() - amount))
                .thenReturn(used);
        // When
        UserPoint result = pointService.usePoint(userId, amount, currentTime);

        // Then
        assertEquals(used.point(), result.point());

        verify(pointHistoryTable).selectAllByUserId(userId);
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable).insert(userId, amount, TransactionType.USE, currentTime);
        verify(userPointTable).insertOrUpdate(userId, origin.point() - amount);
    }

}
