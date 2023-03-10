package com.teee.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.teee.dao.*;
import com.teee.domain.bank.BankWork;
import com.teee.domain.course.Course;
import com.teee.domain.user.UserInfo;
import com.teee.domain.work.Work;
import com.teee.domain.work.WorkSubmit;
import com.teee.domain.work.WorkSubmitContent;
import com.teee.domain.work.WorkTimer;
import com.teee.project.ProjectCode;
import com.teee.service.CourseService;
import com.teee.service.WorkBankService;
import com.teee.service.WorkService;
import com.teee.utils.JWT;
import com.teee.utils.MyAssert;
import com.teee.utils.SpringBeanUtil;
import com.teee.utils.TypeChange;
import com.teee.vo.Result;
import com.teee.vo.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Xu ZhengTao
 * @version 3.0
 */
@Service
@Slf4j
public class WorkServiceImpl implements WorkService {


    @Autowired
    CourseService courseService;
    @Autowired
    CourseUserDao courseUserDao;
    @Autowired
    WorkSubmitDao workSubmitDao;
    @Autowired
    WorkDao workDao;
    @Autowired
    BankWorkDao bankWorkDao;
    @Autowired
    WorkTimerDao workTimerDao;
    @Autowired
    WorkSubmitContentDao workSubmitContentDao;
    @Autowired
    UserInfoDao userInfoDao;
    @Autowired
    CourseDao courseDao;
    @Autowired
    AutoReadOver autoReadOver;



    @Override
    public Result getWorkContent(String token, int id) {
        int role = JWT.getRole(token);
        Work work = workDao.selectById(id);
        MyAssert.notNull(work,"???? ???????????????");
        WorkBankService workBankService= SpringBeanUtil.getBean(WorkBankService.class);
        return workBankService.getWorkBankQuestions(role, work.getBwid());
    }

    @Override
    public Result getQueContent(int wid, int qid) {
        // TODO 4
        Work work = workDao.selectById(wid);
        MyAssert.notNull(work,"???? ???????????????");
        BankWork bankWork = bankWorkDao.selectById(work.getBwid());
        MyAssert.notNull(bankWork, "???? ?????????????????????");
        String bakQue = bankWork.getQuestions().replaceAll(",\\\\\\\"cans\\\\\\\":\\\\\\\".+\\\\\"", "");
        JSONArray arrayList = TypeChange.str2Jarr(bakQue);
        MyAssert.isTrue(qid<=arrayList.size() && qid>0,"??????????????????????????? QAQ");
        return new Result(arrayList.get(qid-1), "????????????");
    }

    @Override
    public Result getWorkTimer(String token, int wid) {
        try{
            // ??????WorkTimer
            Long uid = JWT.getUid(token);
            WorkTimer workTimer = workTimerDao.selectOne(new LambdaQueryWrapper<WorkTimer>().eq(WorkTimer::getUid, uid).eq(WorkTimer::getWid, wid));
            if(workTimer == null){
                /* ???????????????**/
                workTimer = new WorkTimer();
                workTimer.setUid(uid);
                workTimer.setWid(wid);
                Work work = workDao.selectOne(new LambdaQueryWrapper<Work>().eq(Work::getId, wid));
                MyAssert.notNull(work, "???? ?????????????????????????????????????????????");
                try{
                    Float timeLimit = work.getTimeLimit();
                    workTimer.setRestTime(String.valueOf(timeLimit*60.0));
                }catch (NullPointerException npe){
                    workTimer.setRestTime("?????????");
                }
                workTimerDao.insert(workTimer);
            }
            return new Result(ProjectCode.CODE_SUCCESS, workTimer.getRestTime(), "??????Timer??????");
        }catch (Exception e){
            throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "??????Timer?????????", e);
        }
    }

    @Override
    public Result submitWork(String token, JSONObject jo) {
        int wid = jo.getInteger("wid");
        String ans = jo.getString("ans");
        String files= jo.getString("files");
        WorkSubmitContent submitWorkContent = new WorkSubmitContent();
        submitWorkContent.setSubmitContent(ans);
        submitWorkContent.setReadover("");
        submitWorkContent.setFinishReadOver(0);
        //files: [["", "", ""],[]]
        submitWorkContent.setFiles("".equals(files)?"[]":files);
        workSubmitContentDao.insert(submitWorkContent);
        Integer submitId = submitWorkContent.getSid();
        WorkSubmit submitWork = new WorkSubmit();
        Long uid = JWT.getUid(token);
        submitWork.setUid(uid);
        submitWork.setWid(wid);
        submitWork.setScore(0F);
        submitWork.setSid(submitId);
        try {
            UserInfo userInfo = userInfoDao.selectById(uid);
            submitWork.setUname(userInfo.getUname());
            workSubmitDao.delete(new LambdaQueryWrapper<WorkSubmit>().eq(WorkSubmit::getUid, submitWork.getUid()).eq(WorkSubmit::getWid, submitWork.getWid()));
            workSubmitDao.insert(submitWork);
            boolean readChoice = (workDao.selectById(submitWork.getWid()).getAutoReadoverChoice() == 1);
            boolean readFillIn = (workDao.selectById(submitWork.getWid()).getAutoReadoverFillIn() == 1);
            try{
                autoReadOver.autoReadOver(submitWork, readChoice, readFillIn);
            }catch(Exception e){
                throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "??????????????????, ???????????????????????????????????????????????????, ??????????????????????????????", e);
            }
            return new Result(ProjectCode.CODE_SUCCESS, null, "????????????");
        }catch (Exception e){
            throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "???? ????????????????????????????????????", e);

        }
    }

    @Override
    public Result releaseWork(Work work) {
        Course course = courseDao.selectById(work.getCid());
        MyAssert.notNull(course,"?????????????????????");
        // TODO ?????????????????????
        try{
            if ("".equals(work.getDeadline())) {
                work.setDeadline("9999-12-30");
            }
            workDao.insert(work);
            Integer wid = work.getId();
            if(work.getIsExam() == 0){
                ArrayList<String> arrayList = TypeChange.str2arrl(course.getWorks());
                arrayList.add(String.valueOf(wid));
                String s = TypeChange.arrL2str(arrayList);
                course.setWorks(s);
            }else{
                ArrayList<String> arrayList = TypeChange.str2arrl(course.getExams());
                arrayList.add(String.valueOf(wid));
                String s = TypeChange.arrL2str(arrayList);
                course.setExams(s);
            }
            courseDao.updateById(course);
            return new Result(ProjectCode.CODE_SUCCESS, work.getId(), "????????????!");
        }catch(Exception e){
            throw new BusinessException(ProjectCode.CODE_EXCEPTION_BUSSINESS, "???????????????????????????, ???????????????", e);
        }
    }


    @Override
    public Result delWork(JSONObject jo) {
        Work work = workDao.selectOne(new LambdaQueryWrapper<Work>().eq(Work::getId,jo.get("wid")));
        MyAssert.notNull(work, "??????????????????????????????!");
        int i = workDao.deleteById(work.getId());
        MyAssert.isTrue(i>0, "??????????????????????????????!");
        Course course = courseDao.selectById(work.getCid());
        if(work.getIsExam()==1){
            ArrayList<String> arrayList = TypeChange.str2arrl(course.getExams());
            boolean remove = arrayList.remove(work.getId().toString());
            MyAssert.isTrue(remove, "???????????????????????????");
            course.setExams(TypeChange.arrL2str(arrayList));
            courseDao.updateById(course);
        }else{
            ArrayList<String> arrayList = TypeChange.str2arrl(course.getWorks());
            boolean remove = arrayList.remove(work.getId().toString());
            MyAssert.isTrue(remove, "???????????????????????????");
            course.setWorks(TypeChange.arrL2str(arrayList));
            courseDao.updateById(course);
        }
        return new Result(ProjectCode.CODE_SUCCESS, "????????????!");
    }
    // TODO 1

    @Override
    public Result getWorkInfo(JSONObject jo) {
        return null;
    }

    @Override
    public Result editWorkInfo(Work work) {
        return null;
    }



    // TODO 2

    @Override
    public Result getCourseWorkFinishSituation(JSONObject jo) {
        return null;
    }


    @Override
    public Result downloadFiles(Integer wid, HttpServletResponse response) {
        File file = courseService.packageFile(wid);
        if(file == null || !file.exists()){
            throw new BusinessException("???????????????????????????????????????...");
        }else{
            String workName = workDao.selectById(wid).getWname();

            SimpleDateFormat formatter= new SimpleDateFormat("yyyy???MM???dd???'_'HH'???'mm'???'");
            Date date = new Date(System.currentTimeMillis());
            response.reset();
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");
            response.setContentLength((int)file.length());
            try {
                response.setHeader("Content-Disposition", URLEncoder.encode("????????????_" + workName + "_" +formatter.format(date) + ".zip", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                byte[] bytes = FileCopyUtils.copyToByteArray(file);
                OutputStream os = response.getOutputStream();
                os.write(bytes);
            } catch (IOException e) {
                throw new BusinessException("??????????????????");
            }
        }
        return null;
    }

    @Override
    public Result setSubmitScore(JSONObject jo) {
        // TODO ????????????
        return null;
    }

    @Override
    public Result getWorkFinishStatus(String token, int cid) {
        Long uid = JWT.getUid(token);
        JSONArray jarr = (JSONArray) courseService.getWorks(cid).getData();
        JSONArray jarr2 = new JSONArray();

        // [{wid:, status: ,score:}]
        // -1 ?????????
        // 0 ?????????
        // 1 ???????????????
        for (Object o : jarr) {
            JSONObject jo1 =  (JSONObject)o;
            JSONObject jo2 = new JSONObject();
            Integer id = (Integer) jo1.get("id");
            jo2.put("wid", id);
            WorkSubmit submitWork = workSubmitDao.selectOne(new LambdaQueryWrapper<WorkSubmit>().eq(WorkSubmit::getWid, id).eq(WorkSubmit::getUid, uid));
            if(submitWork == null){
                // ?????????
                jo2.put("status", -1);
                jo2.put("score", 0);
            }else{
                if(submitWork.getFinishReadOver() == 0){
                    jo2.put("status", 0);
                    jo2.put("score", submitWork.getScore());
                }else{
                    jo2.put("status", 1);
                    jo2.put("score", submitWork.getScore());
                }
            }
            jarr2.add(jo2);
        }
        return new Result(ProjectCode.CODE_SUCCESS, new ArrayList<String>(jarr2).toString(), "??????????????????????????????!");
    }

    @Override
    public Result getAllWorkSummary(Integer cid) {
        try{
            JSONObject ret = new JSONObject();
            String uids = courseUserDao.selectById(cid).getUid();
            int submitTotalNum = uids.length() - uids.replaceAll(",", "").length() + 1;
            ret.put("submit_totalNum", submitTotalNum);
            ArrayList<JSONObject> arrayList = new ArrayList<>();
            List<Work> works = workDao.selectList(new LambdaQueryWrapper<Work>().eq(Work::getCid, cid));
            for (Work work : works) {
                int readOverDone = workSubmitDao.selectCount(new LambdaQueryWrapper<WorkSubmit>().eq(WorkSubmit::getFinishReadOver, 1).eq(WorkSubmit::getWid,work.getId()));
                int submitSubmitedNum = workSubmitDao.selectCount(new LambdaQueryWrapper<WorkSubmit>().eq(WorkSubmit::getWid, work.getId()));
                JSONObject jo = new JSONObject();
                jo.put("wid", work.getId());
                jo.put("subNum", submitSubmitedNum);
                jo.put("rDone", readOverDone);
                arrayList.add(jo);
            }
            ret.put("works", arrayList);
            return new Result(ProjectCode.CODE_SUCCESS, ret, "????????????");
        }catch (Exception e){
            throw new BusinessException("????????????????????????????????????????????? ...", e);
        }
    }
}
