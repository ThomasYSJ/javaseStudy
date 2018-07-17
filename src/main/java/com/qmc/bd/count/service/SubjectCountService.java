package com.qmc.bd.count.service;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import net.sf.json.JSONObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.qmc.bd.count.model.SubjectCountRecord;
import com.qmc.bd.count.model.SubjectPraiseRecord;
import com.qmc.bd.count.model.SubjectSource;
import com.qmc.redis.RedisCluster;

@Service
public class SubjectCountService {

	private final static Logger logger = LoggerFactory.getLogger(SubjectCountService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Resource
	private RedisCluster jedisCluster;
	
	private String subjectSourceCollectionName = "subject_source";
	private String subjectPraiseRecordCollectionName = "subject_praise_record";
	private String subjectCountRecordCollectionName = "subject_count_record";
	private String prefix_key = "test_c_subject_";
	/**
	 * 点赞
	 * @param parameter
	 * @return
	 */
	public JSONObject givePraise(String parameter){
		JSONObject result = new JSONObject();
		try {
			JSONObject paramJson = JSONObject.fromObject(parameter);
			String source = paramJson.optString("source");
			String subhead = paramJson.optString("subhead");
			String userNo = paramJson.optString("userNo");
			String imei = paramJson.optString("imei");
			
			List<SubjectSource>sourceList = mongoTemplate.find(new Query(Criteria.where("source").is(source)), SubjectSource.class, subjectSourceCollectionName);
			if(sourceList.size()>0){
				SubjectPraiseRecord subjectPraiseRecord = new SubjectPraiseRecord();
				subjectPraiseRecord.setSource(source);
				subjectPraiseRecord.setSubhead(subhead);
				if(StringUtils.isNotBlank(userNo)){
					subjectPraiseRecord.setUserNo(userNo);
				}
				if(StringUtils.isNotBlank(imei)){
					subjectPraiseRecord.setImei(imei);
				}
				subjectPraiseRecord.setCreatetime(new Date());
				mongoTemplate.insert(subjectPraiseRecord, subjectPraiseRecordCollectionName);
				String praiseKey = prefix_key+"praise"+subhead;
				long countPraise = 0;
				if(StringUtils.isNotBlank(userNo)){
					countPraise = jedisCluster.incr(praiseKey);
					
					String praiseKeyUserNo=praiseKey+userNo;
					jedisCluster.set(praiseKeyUserNo, "true");
					
					String praiseKeyImei =praiseKey+imei;//用户编号不为空的时候，需要把imei的缓存标识也加上
					jedisCluster.set(praiseKeyImei, "true");
				}else if(StringUtils.isNotBlank(imei)){
					countPraise = jedisCluster.incr(praiseKey);
					
					String praiseKeyImei=praiseKey+imei;
					jedisCluster.set(praiseKeyImei, "true");
				}
				if(countPraise!=0){
					Update update = new Update();
					update.set("source", source);
					update.set("subhead", subhead);
					update.inc("praise", 1);
					mongoTemplate.upsert(new Query(Criteria.where("subhead").is(subhead)), update, subjectCountRecordCollectionName);
				}
				result.put("errorCode", "0000");
				result.put("message", "成功");
			}else{
				//来源不在来源列表
				result.put("errorCode", "0002");
				result.put("message", "模板来源异常");
			}
		} catch (DuplicateKeyException  e1) {
			result.put("errorCode", "0003");//重复点赞
			result.put("message", "重复点赞");
		} catch(Exception e) {
			result.put("errorCode", "0001");
			result.put("message", "系统错误");
			logger.error("SubjectCountService givePraise "+e.getMessage(),e);
		}
		return result;
	}
	
	/**
	 * 浏览统计
	 * @param parameter
	 * @return
	 */
	public JSONObject browseCount(String parameter){
		JSONObject result = new JSONObject();
		try {
			JSONObject paramJson = JSONObject.fromObject(parameter);
			String source = paramJson.optString("source");
			String subhead = paramJson.optString("subhead");
			String userNo = paramJson.optString("userNo");
			String imei = paramJson.optString("imei");
			List<SubjectSource>sourceList = mongoTemplate.find(new Query(Criteria.where("source").is(source)), SubjectSource.class, subjectSourceCollectionName);
			if(sourceList.size()>0){
				String praiseKey = prefix_key+"praise"+subhead;//subhead点赞总数的key
				String key =prefix_key+"browse"+subhead;//subhead浏览总数的key
				logger.info("browseCount key "+key);
				String countCache = jedisCluster.get(key);
				logger.info("browseCount countCache "+countCache);
				if(countCache==null){
					SubjectCountRecord countList = mongoTemplate.findOne(new Query(Criteria.where("subhead")), SubjectCountRecord.class,subjectPraiseRecordCollectionName);
					if(countList!=null){
						Integer count = countList.getBrowse();
						jedisCluster.set(key, String.valueOf(count));
					}
				}
				Long browseCount =  jedisCluster.incr(key);
				logger.info("browseCount "+browseCount);
				if(browseCount%7==0){
					Update update = new Update();
					update.set("browse", browseCount);
					update.set("source", source);
					update.set("subhead", subhead);
					mongoTemplate.upsert(new Query(Criteria.where("subhead").is(subhead)), update, subjectCountRecordCollectionName);
				}
				result.put("mark", 0);
				
				if(StringUtils.isNotBlank(userNo)){
					String keyUserNo = praiseKey+userNo;
					if(jedisCluster.get(keyUserNo)!=null){
						result.put("mark", 1);
					}
				}
				if(StringUtils.isNotBlank(imei)){
					String keyImei = praiseKey+imei;
					if(jedisCluster.get(keyImei)!=null){
						result.put("mark", 1);
					}
				}
				String praiseCacheValue = jedisCluster.get(praiseKey);
				if(praiseCacheValue==null){
					result.put("praise", 0);
				}else{
					result.put("praise", praiseCacheValue);
				}
				result.put("browse", browseCount);
				result.put("errorCode","0000");
			}else{
				//来源不在来源列表
				result.put("errorCode", "0002");
			}
		} catch (Exception e) {
			result.put("errorCode", "0001");
			logger.info("SubjectCountService browseCount "+e.getMessage(),e);
		}
		return result;
	}
}
