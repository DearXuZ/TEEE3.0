package com.teee.service;


import com.alibaba.fastjson.JSONObject;
import com.teee.domain.course.Course;
import com.teee.vo.Result;

import java.io.File;

/**
 * @author Xu ZhengTao
 * @version 3.0
 */
public interface CourseService {
    Result getCourses(String token, int page);
    /**
     * 获取待批改的TODO列表
     * */
    Result getCoursesTodo(String token);
    Result getCourseInfo(int cid);
    Result createCourse(String token, Course course);
    Result delCourse(int cid);
    Result editCourse(Course course);

    Result addCourse(String token, JSONObject jo);
    Result removeUserFromCourse(Long uid, JSONObject jo);


    Result getUsers(int cid);
    Result getWorks(int cid);
    Result getWorks_(int cid, int page, int isExam);
    Result getAnnouncements(int cid);
    File packageFile(int wid);
}
