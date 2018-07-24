package com.smartcity.meter;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mongodb.MongoClient;

@CrossOrigin
@RestController
public class MeterController {
	@Autowired
	private MongoTemplate mongoTemplate = new MongoTemplate(
			new SimpleMongoDbFactory(new MongoClient("mongo"), "MeterModel"));
	private final String USER_URL = System.getenv("USER_URL");

	MeterController() {
		SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			e.printStackTrace();
		}
		Unirest.setHttpClient(HttpClients.custom().setSSLContext(sslContext)
				.setSSLHostnameVerifier(new NoopHostnameVerifier()).setRetryHandler(new HttpRequestRetryHandler() {
					@Override
					public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
						if (executionCount > 3) {
							return false;
						}
						if (exception instanceof org.apache.http.NoHttpResponseException) {
							return true;
						}
						return false;
					}
				}).build());
	}

	@GetMapping("/collections")
	public @ResponseBody List<MeterModel> getAllCollections(String userType, String userId, String collectionId,
			String[] collectionIds, Boolean open, @RequestParam(defaultValue = "0") Long timestamp, Pageable pageable,
			@RequestParam(defaultValue = "false") Boolean aggregate) {
		if (aggregate && userId != null) {
			Aggregation agg = newAggregation(
					match(Criteria.where("userId").is(userId).and("timestamp").gt(new Date(timestamp))),
					group("collectionId", "type").sum("record").as("record").sum("size").as("size"),
					project("record", "size", "collectionId"), sort(Sort.Direction.DESC, "record"));
			AggregationResults<MeterModel> groupResults = mongoTemplate.aggregate(agg, MeterModel.class,
					MeterModel.class);
			List<MeterModel> result = groupResults.getMappedResults();
			return result;
		}
		Query query = new Query();
		Criteria criteria = new Criteria();
		if (userType != null) {
			criteria = criteria.and("userType").is(userType);
		}
		if (userId != null) {
			criteria = criteria.and("userId").is(userId);
		}
		if (open != null) {
			criteria = criteria.and("isOpen").is(open);
		}
		if (collectionId != null) {
			criteria = criteria.and("collectionId").is(collectionId);
		}
		if (timestamp != null) {
			criteria = criteria.and("timestamp").gt(new Date(timestamp));
		}
		if (collectionIds != null) {
			criteria = criteria.and("collectionId").in((Object[]) collectionIds);
		}
		query.addCriteria(criteria).with(new Sort(Sort.Direction.DESC, "timestamp"));
		query.with(pageable);
		return mongoTemplate.find(query, MeterModel.class);

	}
	
	@SuppressWarnings("rawtypes")
	@PostMapping("/collections")
	public ResponseEntity createCollections(@RequestBody MeterModel meter) {
		mongoTemplate.insert(new MeterModel(meter.getUserType(), meter.getUserId(), meter.getCollectionId(),
				meter.isOpen(), meter.getType(), meter.getRecord(), meter.getSize()));
		return new ResponseEntity(HttpStatus.CREATED);
	}
	@GetMapping("/services")
	public List<?> getAllService(String userType,String userId, String serviceId,
			Boolean open, @RequestParam(defaultValue = "0") Long timestamp, Pageable pageable,
			@RequestParam(defaultValue = "false") Boolean aggregate){
		if (aggregate && userId != null) {
			Aggregation agg = newAggregation(
					match(Criteria.where("userId").is(userId).and("timestamp").gt(new Date(timestamp))),
					group("serviceId").count().as("count"),
					project("count").and("serviceId").previousOperation(), sort(Sort.Direction.DESC, "count"));
			AggregationResults<MeterServiceCount> groupResults = mongoTemplate.aggregate(agg, MeterService.class,
					MeterServiceCount.class);
			List<MeterServiceCount> result = groupResults.getMappedResults();
			return result;
		}
		Query query = new Query();
		Criteria criteria = new Criteria();
		if (userType != null) {
			criteria = criteria.and("userType").is(userType);
		}
		if (userId != null) {
			criteria = criteria.and("userId").is(userId);
		}
		if (open != null) {
			criteria = criteria.and("isOpen").is(open);
		}
		if (serviceId != null) {
			criteria = criteria.and("serviceId").is(serviceId);
		}
		if (timestamp != null) {
			criteria = criteria.and("timestamp").gt(new Date(timestamp));
		}
		query.addCriteria(criteria).with(new Sort(Sort.Direction.DESC, "timestamp"));
		query.with(pageable);
		return mongoTemplate.find(query, MeterService.class);
		
	}
	@SuppressWarnings("rawtypes")
	@PostMapping("/services")
	public ResponseEntity createService(@RequestBody MeterService meter) {
		mongoTemplate.insert(meter);
		return new ResponseEntity(HttpStatus.CREATED);
	}

	

}
