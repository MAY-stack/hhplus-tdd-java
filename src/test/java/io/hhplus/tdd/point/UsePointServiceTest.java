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
import static org.mockito.Mockito.*;

public class UsePointServiceTest {
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
    @DisplayName("포인트 충전 내역이 없는 사용자는 포인트를 사용할 수 없다.")
    void PointHistory가_없는_사용자가_포인트사용을_요청하면_UserNotFoundException이_발생한다() {
        // Given
        long userId = 1L;
        long amount = 100;
        long currentTime = System.currentTimeMillis();
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.emptyList());

        // When
        Exception exception = assertThrows(UserNotFoundException.class,
                () -> pointService.usePoint(userId, amount));

        // Then
        assertEquals("사용자 정보가 없습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }


    @Test
    void PointHistory가_있는_사용자가_1포인트_미만사용을_요청하면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = 1L;
        long amount = 0;
        long currentTime = System.currentTimeMillis();

        PointHistory mockPointHistory = mock(PointHistory.class);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(mockPointHistory));
        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.usePoint(userId, amount));

        // Then
        assertEquals("최소 1포인트부터 사용할 수 있습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    void ID가_0이하의_음수이면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = -1L;
        long amount = 100;

        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.usePoint(userId, amount));
        // Then

        assertEquals("ID는 1 이상 이어야 합니다.", exception.getMessage());
    }

    @Test
    void PointHistory가_있는_사용자가_100만포인트_이상_사용을_요청하면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = 1L;
        long amount = 10000000;
        long currentTime = System.currentTimeMillis();

        PointHistory mockPointHistory = mock(PointHistory.class);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(mockPointHistory));
        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.usePoint(userId, amount));

        // Then
        assertEquals("한번에 최대 100만 포인트까지 사용할 수 있습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("보유한 포인트 이상은 사용할 수 없다.")
    void PointHistory가_있는_사용자가_보유포인트_이상_사용을_요청하면_IllegalArgumentException이_발생한다() {
        // Given
        long userId = 1L;
        long amount = 1000;
        long currentTime = System.currentTimeMillis();
        PointHistory mockPointHistory = mock(PointHistory.class);
        UserPoint mockUserPointOrigin = mock(UserPoint.class);
        when(mockUserPointOrigin.point()).thenReturn(100L);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(mockPointHistory));
        when(userPointTable.selectById(userId)).thenReturn(mockUserPointOrigin);

        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.usePoint(userId, amount));

        // Then
        assertEquals("보유한 포인트를 초과해서 사용할 수 없습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
        verify(userPointTable).selectById(userId);
    }

    @Test
    void PointHistory가_있는_사용자가_1포인트이상_100만포인트이면서_보유포인트_이내로_사용을_요청하면_포인트가_사용된다() {
        // Given
        long userId = 1L;
        long amount = 100;

        PointHistory mockPointHistory = mock(PointHistory.class);
        UserPoint mockUserPointOrigin = mock(UserPoint.class);
        UserPoint mockUserPointUpdated = mock(UserPoint.class);
        when(mockUserPointOrigin.point()).thenReturn(300L);
        when(mockUserPointUpdated.point()).thenReturn(300L - amount);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(mockPointHistory));
        when(userPointTable.selectById(userId))
                .thenReturn(mockUserPointOrigin);
        when(userPointTable.insertOrUpdate(userId, mockUserPointOrigin.point() - amount))
                .thenReturn(mockUserPointUpdated);
        // When
        UserPoint result = pointService.usePoint(userId, amount);

        // Then
        assertEquals(mockUserPointUpdated.point(), result.point());

        verify(pointHistoryTable)
                .selectAllByUserId(userId);
        verify(userPointTable)
                .selectById(userId);
        verify(pointHistoryTable)
                .insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());
        verify(userPointTable)
                .insertOrUpdate(userId, mockUserPointOrigin.point() - amount);
    }

}
