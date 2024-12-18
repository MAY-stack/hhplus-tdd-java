package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.excpetion.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class GetPointHistoryServiceTest {
    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    private PointService pointService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

    @Test
    @DisplayName("충전 내역이 없는 사용자는 조회에 실패한다.")
    void if_history_not_exist_fail(){
        // Given
        long userId = 1L;
        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(Collections.emptyList());

        // When
        Exception exception = assertThrows(UserNotFoundException.class,
                () -> pointService.getPointHistory(userId));

        // Then
        assertEquals("사용자 정보가 없습니다.", exception.getMessage());

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("충전,사용 내역이 있는 사용자는 조회에 성공한다.")
    void success(){
        // Given
        long userId = 1L;
        List<PointHistory> pointHistories = new ArrayList<>();
        pointHistories.add(mock(PointHistory.class));
        pointHistories.add(mock(PointHistory.class));
        pointHistories.add(mock(PointHistory.class));

        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(pointHistories);

        // When
        List<PointHistory> result = pointService.getPointHistory(userId);

        // Then
        assertEquals(pointHistories, result);

        verify(pointHistoryTable).selectAllByUserId(userId);
    }

}
