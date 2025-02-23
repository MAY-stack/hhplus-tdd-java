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

public class GetUserPointServiceTest {
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

        // When
        Exception exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.getUserPoint(userId));
        // Then

        assertEquals("ID는 1 이상 이어야 합니다.", exception.getMessage());
    }

    @Test
    void PointHistory가_없는_사용자의_포인트내역을_조회하면_UserNotFoundException이_발생한다() {
        //  Given
        long userId = 1L;
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.emptyList());

        //  When
        Exception exception = assertThrows(UserNotFoundException.class,
                () -> pointService.getUserPoint(userId));

        //  Then
        assertEquals("사용자 정보가 없습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    void PointHistory가_없는_사용자의_포인트내역을_조회하면_사용자의_PointHistory_리스트가_반환된다() {

        //  Given
        long userId = 1L;

        UserPoint mockUserPoint = mock(UserPoint.class);
        PointHistory mockPointHistory = mock(PointHistory.class);
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(mockPointHistory));
        when(userPointTable.selectById(userId))
                .thenReturn(mockUserPoint);

        //  When
        UserPoint result = pointService.getUserPoint(userId);

        //  Then
        assertEquals(mockUserPoint, result);

        verify(pointHistoryTable).selectAllByUserId(userId);
        verify(userPointTable).selectById(userId);
        verifyNoMoreInteractions(pointHistoryTable, userPointTable);
    }


}
