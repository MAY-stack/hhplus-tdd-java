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
    @DisplayName("내역이 없는 사용자는 포인트가 조회되지 않는다.")
    void if_history_not_exist_fail(){
        //  Given
        long userId = 2L;

        //  When
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.emptyList());

        //  Then
        assertThrows(UserNotFoundException.class,
                ()-> pointService.getUserPoint(userId));

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("내역이 존재하는 사용자는 포인트가 조회된다.")
    void if_history_exist_success(){

        //  Given
        long userId = 1L;
        long amount = 100;
        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        PointHistory pointHistory = new PointHistory(1, userPoint.id(), userPoint.point(), TransactionType.CHARGE, userPoint.updateMillis() );

        //  When
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.singletonList(pointHistory));
        when(userPointTable.selectById(userId))
                .thenReturn(userPoint);
        UserPoint result = pointService.getUserPoint(userId);

        //  Then
        assertEquals(userPoint, result);

        verify(pointHistoryTable).selectAllByUserId(userId);
        verify(userPointTable).selectById(userId);
        verifyNoMoreInteractions(pointHistoryTable, userPointTable);

    }


}