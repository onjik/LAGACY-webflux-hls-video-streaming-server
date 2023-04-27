package com.oj.videostreamingserver.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;

@Component
public class FFmpegProcessUtil {

    @Value("${path.ffmpeg}")
    private String ffmpegPath;

    /**
     * 앞의 ffmpeg를 제외한 커맨드를 전달해주세요
     */
    public boolean executeProcess(String[] inputCommand,boolean wait) throws IOException {
        Assert.notEmpty(inputCommand, "command must not be empty");
        String[] command;
        if (inputCommand[0] != ffmpegPath){
            command = new String[inputCommand.length+1];
            command[0] = ffmpegPath;
            // 기존 배열의 모든 요소를 새로운 배열에 복사합니다.
            System.arraycopy(inputCommand, 0, command, 1, inputCommand.length);
        } else {
            command = inputCommand;
        }

        // 프로세스 실행
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = null;
        process = builder.start();

        if (wait){
            // 명령어 실행이 완료될 때까지 대기
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    return false;
                } else {
                    return true;
                }
            } catch (InterruptedException e) {
                return false;
            }
        } else {
            //대기 안하고 종료
            return true;
        }
    }

}
