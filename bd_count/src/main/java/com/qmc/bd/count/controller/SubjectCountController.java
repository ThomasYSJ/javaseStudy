package com.qmc.bd.count.controller;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qmc.bd.count.service.SubjectCountService;

@Controller
@RequestMapping("/subject")
public class SubjectCountController {

	private static final Logger logger = LoggerFactory.getLogger(SubjectCountController.class);
	
	@Autowired
	private SubjectCountService subjectCountService;
	/**
	 * 浏览统计和返回浏览点赞数
	 * @param parameter
	 * @param callback
	 * @return
	 */
	@RequestMapping(value="/browseCount",produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String browseCount(@RequestParam(value = "parameter")String parameter,
			@RequestParam(value = "callback", required = false) String callback){
		long startTime = System.currentTimeMillis();
		
		JSONObject result = subjectCountService.browseCount(parameter);
        logger.info("SubjectCountController browseCount controller over time: " + (System.currentTimeMillis()-startTime)+ " ms");
        if(StringUtils.isNotBlank(callback)){
			return callback+"("+result.toString()+")";
		}
		return result.toString();
	}
	/**
	 * 点赞接口
	 * @param parameter
	 * @param callback
	 * @return
	 */
	@RequestMapping(value="/givePraise",produces = "application/json;charset=UTF-8")
	@ResponseBody
	public String givePraise(@RequestParam(value = "parameter")String parameter,
			@RequestParam(value = "callback", required = false) String callback){
		long startTime = System.currentTimeMillis();
		
		JSONObject result = subjectCountService.givePraise(parameter);
        logger.info("SubjectCountController givePraise controller over time: " + (System.currentTimeMillis()-startTime)+ " ms");
        if(StringUtils.isNotBlank(callback)){
			return callback+"("+result.toString()+")";
		}
		return result.toString();
	}
}
