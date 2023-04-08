package cn.nkk.hikvision.controller;

import cn.nkk.hikvision.beans.CameraLogin;
import cn.nkk.hikvision.beans.VideoPreview;
import cn.nkk.hikvision.utils.HkUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Author: hsin
 * Date:2023/4/8 12:57
 * Version:1.0
 * Description:
 */
@RestController
public class HikController {

    @GetMapping(value = "/video/rtspReal.flv",produces = {"video/x-flv;charset=UTF-8"})
    public void flvRtspReal(HttpServletResponse response, HttpServletRequest request){

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(0);

        String rtspUrl = HkUtils.toRtspUrl("ip", "推流端口", "账号", "密码",123);
        try {
            HkUtils.rtspToFlv(rtspUrl,asyncContext);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 实时预览
     */
    @GetMapping(value = "/video/sdkReal.flv",produces = {"video/x-flv;charset=UTF-8"})
    public void flvSdkReal(HttpServletResponse response,HttpServletRequest request){

        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(0);

        // sdk抓流，必须登陆
        CameraLogin cameraLogin = HkUtils.doLogin("10.16.36.12", "8000","admin","hik12345");

        // 开启实时预览 （参数二为通道号，可从登陆信息获取到）
        VideoPreview videoPreview = HkUtils.startRelaPlay(cameraLogin.getUserId(), cameraLogin.getChannels().get(0).getChannelNum());
        PipedOutputStream outputStream = videoPreview.getOutputStream();
        PipedInputStream inputStream = new PipedInputStream();
        try {
            inputStream.connect(outputStream);
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(bytes)) != -1) {
                asyncContext.getResponse().getOutputStream().write(bytes);
                response.setContentType("video/x-flv");
                response.setHeader("Connection", "keep-alive");
                response.setStatus(HttpServletResponse.SC_OK);
                response.flushBuffer();
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                asyncContext.complete();
                HkUtils.stopRelaPlay(videoPreview.getPlayHandler()); // 记得关闭预览
                if(outputStream!=null) outputStream.close();
                if(inputStream!=null) inputStream.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
