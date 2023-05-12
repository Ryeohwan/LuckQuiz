package com.luckquiz.quizroom.api.controller;


import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.GoogleCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.*;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.luckquiz.quizroom.api.request.Grade;
import com.luckquiz.quizroom.api.request.QuizStartRequest;
import com.luckquiz.quizroom.api.response.Duplucheck;
import com.luckquiz.quizroom.api.response.QGame;
import com.luckquiz.quizroom.api.response.ToGradeStartMessage;
import com.luckquiz.quizroom.api.service.*;
import com.luckquiz.quizroom.exception.CustomException;
import com.luckquiz.quizroom.exception.CustomExceptionType;
import com.luckquiz.quizroom.message.EmotionResultMessage;
import com.luckquiz.quizroom.message.EnterGuestMessage;
import com.luckquiz.quizroom.message.QuizStartMessage;
import com.luckquiz.quizroom.model.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.RestController;

// 꾸글
//

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    private final SimpMessageSendingOperations sendingOperations;
    private  final StringRedisTemplate stringRedisTemplate;
    private final Gson gson;
    private final ToGradeProducer toGradeProducer;
    private final ToQuizProducer toQuizProducer;
    private final QuizService quizService;
    private final ClovarVisionService clovarVisionService;

    @MessageMapping("/enter")
    public void enter(QuizMessage message) {
        Grade grade = new Grade();
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        System.out.println("entered:  "+message.getSender());
        int roomId = message.getRoomId();
        grade.setPlayerName(message.getSender());
        grade.setPlayerImg(message.getImg());
        hashOperations.put(roomId+"p", message.getSender(), gson.toJson(grade));
        EnterUser enterUser = EnterUser.builder()
                .sender(message.getSender())
                .img(message.getImg())
                .build();
        zSetOperations.add(roomId+"rank",gson.toJson(enterUser),0);
        stringStringValueOperations.append(roomId+"l",gson.toJson(enterUser)+", ");

        String allList = stringStringValueOperations.get(roomId+"l",0,-1);
        String [] arr = allList.split(", ");
        List<EnterUser> result = new ArrayList();
        for(String user: arr){
            EnterUser a = gson.fromJson(user,EnterUser.class);
            result.add(a);
        }

        LinkedHashSet<EnterUser> li = new LinkedHashSet<EnterUser>(result);
        result.clear();
        result.addAll(li);

        EnterGuestMessage egm = EnterGuestMessage.builder()
                .type("enterGuestList")
                .enterGuestList(result)
                .build();

        sendingOperations.convertAndSend("/topic/quiz/" + message.getRoomId(), egm);
        quizService.serveEntry(egm,message.getRoomId());
    }

    @MessageMapping("/duplicheck")
    public void duplicheck(QuizMessage message) {
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        String allList = stringStringValueOperations.get(message.getRoomId()+"l",0,-1);
        String [] arr = allList.split(", ");
        String check = "true";
        for(String user: arr){
            EnterUser a = gson.fromJson(user,EnterUser.class);
            if(a.getSender().equals(message.getSender())){
                check = "false";
            }
        }
        Duplucheck d = new Duplucheck();
        d.setType("checkGuestName");
        d.setCheckGuestName(check);
        sendingOperations.convertAndSend("/queue/quiz/" + message.getRoomId()+"/"+message.getSender(), d);
    }


    @MessageMapping("/emotion")
    public void emotion(QuizMessage message) throws  Exception{
        byte[] decode;
        EmotionResultMessage result;
        try{
            decode =  Base64.getDecoder().decode(message.getFile().split(",")[1]);
            if(decode.length>= 2097152){
                throw new CustomException(CustomExceptionType.FILE_TOO_LARGE);
            }
            result = clovarVisionService.naverCheck(decode);
            sendingOperations.convertAndSend("/queue/quiz/" + message.getRoomId()+"/"+message.getSender(), result);
            // 채점결과 채점서버로 message 보내기
            toGradeProducer.clientSubmit(gson.toJson(message));
            log.info("제출 했습니다.");
        } catch (CustomException e){
            throw new CustomException(CustomExceptionType.NO_PICTURE_ERROR);
        }


        // 구글의 얼굴인식 api
//        GoogleVisionService googleVisionService = new GoogleVisionService(gson);
//        googleVisionService.googleCheck(decode);


//      System.out.println("submited:   "+message.getHostId()+", sender:    "+message.getSender());


//      Object resultRespone = gson.fromJson(result, Object.class);
//      sendingOperations.convertAndSend("/queue/quiz/" + message.getRoomId()+"/"+message.getSender(), result);
//        sendingOperations.convertAndSend("/queue/quiz/" + message.getRoomId()+"/"+message.getSender(), result);
    }

    @MessageMapping("/submit")
    public void submit(QuizMessage message) throws  Exception{
//      System.out.println("submited:   "+message.getHostId()+", sender:    "+message.getSender());
//      toGradeProducer.clientSubmit(gson.toJson(message));
//      Object resultRespone = gson.fromJson(result, ResultRes.class);
//      sendingOperations.convertAndSend("/queue/quiz/" + message.getRoomId()+"/"+message.getSender(), result);
    }

//    @MessageMapping("/{roomId}/private/{sender}")
//    // 각각의 사용자에게 채점 결과를 보내주는 메소드
//    public void quizSpread(@Payload QuizMessage message,@DestinationVariable int roomId, @DestinationVariable String sender, Principal principal) {
//        // 메시지를 수신자에게 전송
//        System.out.println("private here");
//        sendingOperations.convertAndSendToUser(sender, "/queue/"+roomId+"/private/", message);
//    }

    // 퀴즈가 시작 요청이 오면 맨 처음 문제를 반환한다.
    // 이 때 quizNum 이 0으로 초기화된다.
    @MessageMapping("/quiz/start")
    public void start(QuizStartRequest quizStartRequest) {
        QGame result = quizService.startQuiz(quizStartRequest);
        ToGradeStartMessage toGradeStartMessage = ToGradeStartMessage.builder()
                .quizNum(result.getQuizNum())
                .hostId(quizStartRequest.getHostId())
                .roomId(quizStartRequest.getRoomId())
                .build();
        String type = "";
        if(result.getQuiz() != null){
            type = result.getQuiz();
        }else{
            type = result.getGame();
        }
        QuizStartMessage qsm = QuizStartMessage.builder()
                .type("getQuizItem")
                .getQuizItem(result)
                .build();
        toGradeProducer.quizStart(gson.toJson(toGradeStartMessage));
        sendingOperations.convertAndSend("/topic/quiz/" + quizStartRequest.getRoomId(), qsm);

        // 참가자들한테 메세지 뿌리기
        QGame toGuest = QGame.serveQgame(result);
        QuizStartMessage qsmG = QuizStartMessage.builder()
                .type("getQuizItem")
                .getQuizItem(toGuest)
                .build();
        quizService.serveQuiz(qsmG,quizStartRequest.getRoomId());
    }

    @MessageMapping("/quiz/next")
    public void next(NextMessage nextMessage) {
        QGame result = quizService.nextQuiz(nextMessage);
        String type = "";
        if(result.getQuiz() != null){
            type = result.getQuiz();
        }else{
            type = result.getGame();
        }
        QuizStartMessage qsm = QuizStartMessage.builder()
                .type("getQuizItem")
                .getQuizItem(result)
                .build();
        sendingOperations.convertAndSend("/topic/quiz/" + nextMessage.getRoomId(), qsm);
        //퀴즈 다음페이지 넘기기.

        // 참가자들한테 메세지 뿌리기
        QGame toGuest = QGame.serveQgame(result);
        QuizStartMessage qsmG = QuizStartMessage.builder()
                .type("getQuizItem")
                .getQuizItem(toGuest)
                .build();
        quizService.serveQuiz(qsmG,nextMessage.getRoomId());
    }

    @MessageMapping("/quiz/end")
    public void quizEnd(QuizMessage message) {
//        세션 끝내면 저장한것도 삭제
        toQuizProducer.QuizEnd(gson.toJson(message));
    }

    @MessageMapping("/quiz/currentCount")
    public void currentParticipent(CurrentParticipent currentParticipent){
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        Map all = hashOperations.entries(currentParticipent.getRoomId()+"p");
        sendingOperations.convertAndSend("/topic/quiz/" + currentParticipent.getRoomId(), all.size());
    }

    @MessageMapping("/quiz/execute")
    public void execute(ShutDownRequest shutDownRequest){
        String hashKey = shutDownRequest.getRoomId()+"p";
        String zsetKey = shutDownRequest.getRoomId()+"rank";
        stringRedisTemplate.delete(shutDownRequest.getRoomId().toString());
        stringRedisTemplate.delete(hashKey);
        stringRedisTemplate.delete(zsetKey);
    }

    @MessageMapping("/quiz/rollback")
    public void rollBack(RollBackRequest rollBackRequest){
        if(rollBackRequest.getRoomId() != null) toGradeProducer.rollBack(gson.toJson(rollBackRequest));
    }

    @MessageMapping("/quiz/middlerank")
    public void rollBack(MiddleRank middleRank){
        HashOperations<String, String, String> hashOperations = stringRedisTemplate.opsForHash();
        Map all = hashOperations.entries(middleRank.getRoomId()+"p");
        List<String> users = new ArrayList<>(all.values());
        List<UserR> userLList = new ArrayList<>();
        for(String user: users){
            Grade a = gson.fromJson(user,Grade.class);
            UserR u = new UserR();
            u.setImg(a.getPlayerImg());
            u.setSender(a.getPlayerName());
            u.setRank(a.getRankNow());
            userLList.add(u);
        }
        Collections.sort(userLList);
        sendingOperations.convertAndSend("/topic/quiz/" + middleRank.getRoomId(), userLList);
    }

    @MessageMapping("/quiz/ranking")
    public void totalRank (TotalRank totalRank){
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        Set<String> all = zSetOperations.range(totalRank.getRoomId()+"rank",0,zSetOperations.size(totalRank.getRoomId()+"rank")-1);
        List<String> rank = new ArrayList<>(all);
        List<UserR> result = new ArrayList<>();
        int rankNum = 1;
        for(String name : rank){
            UserR tempU = new UserR();
            String uName = name.split(" ")[0];
            int img = Integer.parseInt(name.split(" ")[1]);
            tempU.setSender(name);
            tempU.setImg(img);
            tempU.setRank(rankNum);
            rankNum ++;
        }
        sendingOperations.convertAndSend("/topic/quiz/" + totalRank.getRoomId(),result);
        sendingOperations.convertAndSend("/queue/quiz/" + totalRank.getRoomId(),result);
    }



}