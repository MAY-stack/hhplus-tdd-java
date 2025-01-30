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
import static org.mockito.Mockito.*;

public class ChargePointServiceTest {

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    private PointService pointService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

    @Test
    void ID가_0이하의_음수이면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = -1L;
        long amount = 100;

        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoint(userId, amount));
        // Then

        assertEquals("ID는 1 이상 이어야 합니다.", exception.getMessage());
    }

    @Test
    void 포인트를_1미만으로_충전하면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = 1L;
        long amount = -10L;

        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoint(userId, amount));
        // Then

        assertEquals("1포인트 미만은 충전할 수 없습니다.", exception.getMessage());
    }

    @Test
    void 한번에_100_000_포인트_이상을_충전하면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = 1L;
        long amount = 200000L;

        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoint(userId, amount));

        // Then
        assertEquals("한번에 10만 포인트를 초과해서 충전할 수 없습니다.", exception.getMessage());
    }

    @Test
    void 충전_후_포인트가_1억_포인트를_초과하면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = 1L;
        long amount = 90000;

        UserPoint mockUserPoint = mock(UserPoint.class);
        when(mockUserPoint.point()).thenReturn(99999999L);
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);
        // When

        // Then
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoint(userId, amount));
        assertEquals("1억 포인트를 초과해서 보유할 수 없습니다."
                , exception.getMessage());

        verify(userPointTable).selectById(userId);
    }

    @Test
    void 포인트_1이상_100만_이하를_충전하면_충전처리를_하고_충전한_UserPoint를_반환한다() {
        // Given
        long userId = 1L;
        long amount = 100;

        UserPoint mockUserPointOrigin = mock(UserPoint.class);
        UserPoint mockUserPointUpdated = mock(UserPoint.class);
        PointHistory mockPointHistory = mock(PointHistory.class);
        when(mockUserPointOrigin.point())
                .thenReturn(500L);
        when(mockUserPointUpdated.point())
                .thenReturn(500L + amount);
        when(userPointTable.selectById(userId))
                .thenReturn(mockUserPointOrigin);
        when(pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis()))
                .thenReturn(mockPointHistory);
        when(userPointTable.insertOrUpdate(userId, mockUserPointUpdated.point()))
                .thenReturn(mockUserPointUpdated);

        // When
        UserPoint result = pointService.chargePoint(userId, amount);

        // Then
        assertEquals(mockUserPointUpdated.point(), result.point());

        verify(userPointTable)
                .selectById(userId);
        verify(pointHistoryTable)
                .insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
        verify(userPointTable)
                .insertOrUpdate(userId, mockUserPointUpdated.point());
    }

}
