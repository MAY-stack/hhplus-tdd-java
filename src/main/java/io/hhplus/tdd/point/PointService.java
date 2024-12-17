package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.excpetion.UserNotFoundException;

public class PointService {

    private PointHistoryTable pointHistoryTable;
    private UserPointTable userPointTable;

    PointService (PointHistoryTable pointHistoryTable, UserPointTable userPointTable){
        this.pointHistoryTable = pointHistoryTable;
        this.userPointTable = userPointTable;
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
    
}
