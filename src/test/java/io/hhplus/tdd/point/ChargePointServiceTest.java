package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChargePointServiceTest {
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
    @DisplayName("1 포인트 미만은 충전할 수 없다.")
    void if_under_one_point_charge_should_fail(){
        // Given
        long userId = 1L;
        long amount = -10L;
        long currentTime = System.currentTimeMillis();

        // When

        // Then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoint(userId, amount, currentTime));
        assertEquals("1포인트 미만은 충전할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("100_000 포인트 이상은 충전할 수 없다.")
    void if_over_one_hundred_thousand_point_charge_should_fail(){
        // Given
        long userId = 1L;
        long amount = 200000L;
        long currentTime = System.currentTimeMillis();

        // When

        // Then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoint(userId, amount, currentTime));
        assertEquals("한번에 10만 포인트를 초과해서 충전할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("100_000_000 포인트 이상은 보유할 수 없다.")
    void can_have_over_one_hundred_million_point(){
        // Given
        long userId = 1L;
        long amount = 90000;
        long currentTime = System.currentTimeMillis();

        UserPoint origin = new UserPoint(userId, 99999999L, currentTime);
        when(userPointTable.selectById(userId)).thenReturn(origin);
        // When

        // Then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoint(userId, amount, System.currentTimeMillis()));
        assertEquals("1억 포인트를 초과해서 보유할 수 없습니다.", exception.getMessage());

        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("성공 케이스")
    void success(){
        // Given
        long userId = 1L;
        long amount = 100;
        long currentTime = System.currentTimeMillis();
        UserPoint origin = new UserPoint(userId, 500, System.currentTimeMillis());
        UserPoint charged = new UserPoint(userId, origin.point() + amount, currentTime);
        PointHistory pointHistory = new PointHistory(1, userId, amount, TransactionType.CHARGE, currentTime);
        when(userPointTable.selectById(userId))
                .thenReturn(origin);
        when(pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, currentTime))
                .thenReturn(pointHistory);
        when(userPointTable.insertOrUpdate(userId, charged.point()))
                .thenReturn(charged);

        // When
        UserPoint result = pointService.chargePoint(userId, amount, currentTime);

        // Then
        assertEquals(charged.point(), result.point());
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable)
                .insert(userId, amount, TransactionType.CHARGE, currentTime);
        verify(userPointTable).insertOrUpdate(userId, charged.point());
    }

}
