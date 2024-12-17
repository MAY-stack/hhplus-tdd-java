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

    /**
     *  User의 포인트를 충전한다.
     */
    UserPoint chargePoint(long id, long amount, long updateMillis) throws RuntimeException {
        if(amount < 1){
            throw new IllegalArgumentException("1포인트 미만은 충전할 수 없습니다.");
        }
        if(amount > 100000){
            throw new IllegalArgumentException("한번에 10만 포인트를 초과해서 충전할 수 없습니다.");
        }
        UserPoint origin = userPointTable.selectById(id);
        if(origin.point() + amount > 100000000){
            throw new IllegalArgumentException("1억 포인트를 초과해서 보유할 수 없습니다.");
        }
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, updateMillis);
        return userPointTable.insertOrUpdate(id, origin.point() + amount);
    }

}
