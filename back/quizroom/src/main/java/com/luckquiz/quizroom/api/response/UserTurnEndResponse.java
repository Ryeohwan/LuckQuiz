package com.luckquiz.quizroom.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserTurnEndResponse {
    private int scoreGet;
    private String isUp;
    private int rankDiff;
    private int quizNum;
    private int rankNow;
    private int totalScore;
    private int totalRankNow;
    private int totalRankPre;

    public void setScoreGet(int scoreGet){
        this.scoreGet = scoreGet;
    }
    public void setIsUp(String isUp){
        this.isUp = isUp;
    }
    public void setRankDiff(int rankDiff){
        this.rankDiff = rankDiff;
    }
    public void setQuizNum(int quizNum){this.quizNum = quizNum;}

    public void setRankNow(int rankNow) {
        this.rankNow = rankNow;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }
    public void setTotalRankNow(int totalRankNow) {
        this.totalRankNow = totalRankNow;
    }
    public void setTotalRankPre(int totalRankPre) {
        this.totalRankPre = totalRankPre;
    }
}
