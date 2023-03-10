package com.teee.controller.Bank;

import com.teee.domain.bank.BankWork;
import com.teee.project.Annoation.RoleCheck;
import com.teee.project.ProjectRole;
import com.teee.service.WorkBankService;
import com.teee.utils.JWT;
import com.teee.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/banks/work")
public class WorkBankController {

    @Autowired
    WorkBankService workBankService;

    @PostMapping
    @RoleCheck(role = ProjectRole.TEACHER)
    public Result addWorkBank(@RequestHeader("Authorization") String token, @RequestBody BankWork bankWork){
        Long tid = JWT.getUid(token);
        return workBankService.createWorkBank(bankWork, tid);
    }
    @GetMapping
    @RoleCheck(role = ProjectRole.TEACHER)
    public Result getMyWorkBank(@RequestHeader("Authorization") String token){
        return workBankService.getWorkBankByOnwer(JWT.getUid(token));
    }

    @GetMapping("/content")
    public Result getWorkBankContentById(@RequestHeader("Authorization") String token,@RequestParam("bwid") Integer bankWorkId){
        return workBankService.getWorkBankContent(JWT.getUid(token),bankWorkId);
    }
    @GetMapping("/questions")
    @RoleCheck(role = ProjectRole.TEACHER)
    public Result getWorkBankQuestions(@RequestHeader("Authorization") String token,@RequestParam("bwid") Integer bankWorkId){
        return workBankService.getWorkBankQuestions(JWT.getRole(token), bankWorkId);
    }
    @PutMapping
    @RoleCheck(role = ProjectRole.TEACHER)
    public Result editWorkBank(@RequestBody BankWork bankWork){
        return workBankService.editWorksBank(bankWork);
    }

    @DeleteMapping
    @RoleCheck(role = ProjectRole.TEACHER)
    public Result deleteWorkBank(Integer wbid){
        return workBankService.deleteWorkBank(wbid);
    }

}
