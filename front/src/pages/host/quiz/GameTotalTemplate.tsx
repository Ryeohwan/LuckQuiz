import React from 'react';
import styles from './GameTotalTemplate.module.css'
import wakeup from '../../../assets/images/wakeup_game.png'
import { useSelector } from 'react-redux';
import { RootState } from 'store';
import GameEmotionTemplate from './GameEmotionTemplate';
// 게임 재사용 페이지
const GameTotalTemplate = () => {

    const quizInfo = useSelector((state: RootState) => state.quiz);
    const selectInfo = useSelector((state: RootState) => state.auth);


    return (
        <>
        {
            quizInfo.quizList[selectInfo.choiceType].game==="emotion"? <GameEmotionTemplate/>: <div className={styles.shake_content}>
            <div className={styles.shake_title}>
                <h1>일어나 쿼카야 학교 가야지</h1>
                <div className={styles.shake_explain}>상단 메뉴에서 게임이 진행될 시간을 선택하세요</div>
            </div>
            <div className={styles.shake_img}><img src={wakeup} alt="쿼카" /></div>
        </div>
        }
           


        </>
    );
};

export default GameTotalTemplate;