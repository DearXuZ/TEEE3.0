package com.teee.controller;

import com.teee.project.ProjectCode;
import com.teee.utils.MyAssert;
import com.teee.utils.TypeChange;
import com.teee.vo.Result;
import com.teee.vo.UploadErr;
import com.teee.vo.UploadResult;
import com.teee.vo.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Xu ZhengTao
 * @version 3.0
 */
@Controller
@RequestMapping("/upload")
@Slf4j
public class UploadController {
    @Value("${path.pic.works}")
    private String worksPicPath;

    @Value("${path.pic.faces}")
    private String facesPicPath;

    @Value("${path.file.files}")
    private String filePath;

    @Value("${path.file.temps}")
    private String tempsPath;

    @Value("${server.port}")
    private String port;


    ArrayList<String> suffixWhiteList;

    public UploadController() {
        suffixWhiteList= new ArrayList<>();
        suffixWhiteList.add(".png");
        suffixWhiteList.add(".jpg");
        suffixWhiteList.add(".jpeg");
        suffixWhiteList.add(".gif");
        suffixWhiteList.add(".ico");
        suffixWhiteList.add(".cur");
        suffixWhiteList.add(".jfif");
        suffixWhiteList.add(".pjpeg");
        suffixWhiteList.add(".pjp");
        suffixWhiteList.add(".svg");
        suffixWhiteList.add(".tif");
        suffixWhiteList.add(".webp");
        suffixWhiteList.add(".tiff");
    }

    private  UploadResult upload(MultipartFile file, HttpServletRequest request, String path, String dirName, boolean isPic){
        if(file == null){
            return new UploadResult(0, new UploadErr("????????????????????????"));
        }
        String originalFilename = file.getOriginalFilename();
        MyAssert.notNull(originalFilename, "?????????????????????");
        assert originalFilename != null;
        String appendName = originalFilename.substring(originalFilename.lastIndexOf("."));
        if(isPic && !suffixWhiteList.contains(appendName)){
            return new UploadResult(0, new UploadErr("?????????????????????"));
        }
        File newMkdir = new File(path);
        if(!newMkdir.exists()){
            boolean mkdirs = newMkdir.mkdirs();
            MyAssert.isTrue(mkdirs, "???????????????????????? ... ");
        }
        String uploadFile = System.currentTimeMillis() + appendName;
        try {
            file.transferTo(new File(path+File.separator+uploadFile));
            String url = request.getScheme() + "://" + request.getServerName() + ":" + port + "/" + dirName + "/" + uploadFile;
            return new UploadResult(1, uploadFile,url);
        } catch (IOException e) {
            e.printStackTrace();
            return new UploadResult(0, new UploadErr("????????????"));
        }
    }
    @RequestMapping("/file")
    @ResponseBody
    public Result uploadFile(@RequestParam("file") MultipartFile[] file){
        try{
            log.info("?????????????????? ...");

            ArrayList<String> arrayList = new ArrayList<>();
            for (MultipartFile multipartFile : file) {
                if(multipartFile.isEmpty()){
                    throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "???????????????????????????");
                }
                String fileName = multipartFile.getOriginalFilename();
                if(fileName == null){
                    fileName = UUID.randomUUID().toString();
                }
                int point = fileName.lastIndexOf(".");
                String suffixName = point>0?fileName.substring(point):"";
                log.info("??????: " + fileName + ", ??????: " + suffixName);
                File fileTempObj = new File(filePath + File.separator + "TimeStamp_" +  System.currentTimeMillis() + "_" + fileName);
                if(!fileTempObj.getParentFile().exists()){
                    boolean mkdirs = fileTempObj.getParentFile().mkdirs();
                    MyAssert.isTrue(mkdirs, "???????????????????????????????????????");
                }
                try{
                    multipartFile.transferTo(fileTempObj);
                }catch (Exception e){
                    throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "??????????????????:"+ fileTempObj.getName(), e);
                }
                arrayList.add("\"" + fileTempObj.getName() + "\"");
            }
            return new Result(ProjectCode.CODE_SUCCESS, TypeChange.arrL2str(arrayList), "???????????????");
        }catch (Exception e){
            throw new BusinessException(ProjectCode.CODE_EXCEPTION_SYSTEM, "???????????????????????????????????? ... ", e);
        }

    }

    @RequestMapping("/works")
    @ResponseBody
    public UploadResult uploadWorkImg(@RequestParam("upload") MultipartFile file, HttpServletRequest request){
        return upload(file, request, worksPicPath, "pic_works", true );
    }
    @RequestMapping("/faces")
    @ResponseBody
    public UploadResult uploadFaceImg(@RequestParam("upload") MultipartFile file, HttpServletRequest request){
        return upload(file, request, facesPicPath, "pic_faces", true );
    }
    @RequestMapping("/files")
    @ResponseBody
    public UploadResult uploadFiles(@RequestParam("upload") MultipartFile file, HttpServletRequest request){
        return upload(file, request, filePath, "file_files", false );
    }
    @RequestMapping("/temps")
    @ResponseBody
    public UploadResult uploadTempFiles(@RequestParam("upload") MultipartFile file, HttpServletRequest request){
        return upload(file, request, tempsPath, "file_temp", false );
    }
    @RequestMapping("/getFile")
    @ResponseBody
    public Result downloadFile(@RequestParam("fileName") String fileName, HttpServletResponse response) throws UnsupportedEncodingException {
        File file = new File(filePath + File.separator + fileName);
        String substring = fileName.substring(fileName.lastIndexOf("_")+1);
        String fileOriginName = substring.substring(substring.lastIndexOf("_")+1);
        if(!file.exists()){
            throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "????????????????????????????");
        }
        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int)file.length());
        response.setHeader("Content-Disposition", URLEncoder.encode(fileOriginName, "UTF-8"));
        long startTime = System.currentTimeMillis();
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(file);
            OutputStream os = response.getOutputStream();
            os.write(bytes);
            os.close();
        } catch (IOException e) {
            throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "?????????????????????????");
        }

        return null;
    }

}
