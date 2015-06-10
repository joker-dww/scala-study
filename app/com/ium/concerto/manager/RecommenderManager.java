package com.ium.concerto.manager;

import com.ium.concerto.config.WebAppInitializer;
import com.ium.concerto.domain.*;
import com.ium.concerto.enums.Gender;
import com.ium.concerto.enums.SimilarityAlgorithm;
import com.ium.concerto.model.*;
import com.ium.concerto.recommender.IumRecommender;
import com.ium.concerto.recommender.IumUserBasedRecommender;
import com.ium.concerto.recommender.WeightedRecommenderPicker;
import com.ium.concerto.service.PeopleGroupAssignService;
import com.ium.concerto.service.ReservedMatchConditionService;
import com.ium.concerto.service.recommender.*;
import com.ium.concerto.util.DateUtil;
import com.ium.concerto.util.PersonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class RecommenderManager {
	private final Log logger = LogFactory.getLog(getClass());
	
	@Autowired
	private PreferenceService preferenceService;
	
	@Autowired
	private RecommenderService recommenderService;
	
	@Autowired
	private AlgorithmService algorithmService;
	
	@Autowired
	private ReservedMatchConditionService reservedMatchConditionService;
	
	@Autowired
	private IumMatchWriter iumMatchWriter;

	@Autowired
	private RecommenderStatusService recommenderStatusService;
	
	@Autowired
	private PeopleGroupAssignService peopleGroupAssignService;
	
	@Autowired
	private IumPeopleService iumPeopleService;	
	
	@Autowired
	private MessageService messageService;
	
	@Autowired
	private PeopleMatchManager peopleMatchManager;

    @Autowired
    private IumMatchReader iumMatchReader ;

	// 권역별 회원 정보
	private Map<String, FastByIDMap<Person>> regions;

	// 담당자 번호
	private String[] phones = {
            "01067058088",  // akki
            "01045430186",  // nalssing
            "07088730680",  // seba
            "01083511840",  // joker
			"01054622792",  // oz
            "01026380641", // Joe
            "01054906080",  // jambo
		};
	
				
	public void addBestRecommenderAlgorithm(Gender gender) throws TasteException {
	}
	
	
	/**
	 * 메일 새벽에 실행
	 * @throws ParseException 
	 * 
	 */
	@Scheduled(cron="0 0 5 * * ?")
	public void scheduledMatchFirst() throws ParseException {
        Properties appProperties = getAppProperties() ;
        String profile = appProperties.getProperty("app.profile") ;

        if (profile.equals("release") == false)
            return ;

		ReservedMatchCondition reservedMatchCondition = reservedMatchConditionService.getActivatedReservedMatchCondition();
		
		String matchDate = DateUtil.SDF_SHORT_DB.format(DateUtil.removeTime(Calendar.getInstance()).getTime());

		startMatching(reservedMatchCondition.getSeqId(), matchDate + " 12:30:00");
	}

	/**
	 * 메일 오후 2시 30분에 실행
	 * @throws ParseException
	 *
	 */
	@Scheduled(cron="0 30 14 * * ?")
	public void scheduledMatchSecond() throws ParseException {
        Properties appProperties = getAppProperties() ;
        String profile = appProperties.getProperty("app.profile") ;

        if (profile.equals("release") == false)
            return ;

		ReservedMatchCondition reservedMatchCondition = reservedMatchConditionService.getActivatedReservedMatchCondition();

		String matchDate = DateUtil.SDF_SHORT_DB.format(DateUtil.removeTime(Calendar.getInstance()).getTime());

		startMatching(reservedMatchCondition.getSeqId(), matchDate + " 18:00:00");
	}

    @Async
    public void preprocess(long reservedMatchConditionSeqId, String matchDate) throws ParseException {
        peopleMatchManager.preprocess(matchDate);
        peopleMatchManager.peopleGroupAssigning(reservedMatchConditionSeqId);
    }

	@Async
	public void startMatching(long reservedMatchConditionSeqId, String matchDate) {
		Gender gender = Gender.FEMALE;

		recommenderService.setMatchDate(matchDate);
		String startDate = DateUtil.SDF_FULL_DB.format(Calendar.getInstance().getTime());
		
		try {
            Integer iumMatchId = iumMatchReader.getIumMatchId(matchDate) ;
            if (iumMatchId != null && iumMatchId > 0) {
                Integer matchElementCount = iumMatchReader.getMatchElementCount(iumMatchId) ;
                if (matchElementCount != null && matchElementCount > 0) {
                    logger.debug(String.format("match_date: %s ium_match %d exists. match_element count: %d", matchDate, iumMatchId, matchElementCount)) ;
                    messageService.sendSms(phones, "[콘체] " + matchDate + " 매칭 이미 완료되어 매칭하지 않을래요~") ;

                    return ;
                }
            }

            recommenderStatusService.start();
			logger.info("매칭 시작: " + matchDate) ;
            messageService.sendSms(phones, "[콘체] " + matchDate + " 매칭 시작합니다요 ~ ");

			long startTime = System.nanoTime();
			
			// Assign to Group
            logger.info("Start Preprocessing");
            peopleMatchManager.preprocess(matchDate);

			logger.info("start Assign to Group");
			peopleMatchManager.peopleGroupAssigning(reservedMatchConditionSeqId);
			logger.info("end Assign to Group");
			
			ReservedMatchCondition reservedMatchCondition = reservedMatchConditionService.getReservedMatchCondition(reservedMatchConditionSeqId);

			List<PeopleGroupAssign> assignedPeopleMetaDataList = peopleGroupAssignService.getAssignedPeopleMetaDataList();

			List<Person> persons = new ArrayList<>();
			
			FastByIDMap<Person> males = new FastByIDMap<>();
			FastByIDMap<Person> females = new FastByIDMap<>();

			Person person = null;
			PeopleMetaData peopleMetaData = null;
			GroupMetaData groupMetaData = null;
			
			recommenderStatusService.startWork("회원 메타 데이터 가져오는중", assignedPeopleMetaDataList.size());
			regions = new HashMap<>();

            int i = 0;
			for(PeopleGroupAssign peopleGroupAssign : assignedPeopleMetaDataList) {
				peopleMetaData = peopleGroupAssign.getPeopleMetaData();
				groupMetaData = peopleGroupAssign.getGroupMetaData();

                if (++i % 1000 == 0)
                    logger.info("메타데이터 가져오는중... " + i);

				person = new Person();
				person.setId(peopleMetaData.getIumPeople());
				person.setAge(PersonUtils.getAge(peopleMetaData.getBirthYear()));
				person.setArea(peopleMetaData.getArea());
				person.setGender(peopleMetaData.getGender());
                person.setFriends(iumPeopleService.getFriends(person.getId()));
                person.setGroup(new Group(groupMetaData.getSeqId(), groupMetaData.getGroupName()));

                PastPartners pastPartners = iumPeopleService.getPastPartners(person.getGender(), person.getId());
                person.setPastPartners(pastPartners.getPartners());
                person.setSatisfaction(pastPartners.getSatisfaction());
                person.setCheckRate(pastPartners.getCheckRate());

				persons.add(person);
				
				recommenderStatusService.addCurrent();
			}

            logger.info("매치 대상 유저 소팅중");
			Collections.sort(persons, new Comparator<Person>() {
				@Override
				public int compare(Person p1, Person p2) {
					if (p1.getCheckRate() > p2.getCheckRate()) {
						return -1;
					}

                    else if (p1.getCheckRate() < p2.getCheckRate()) {
						return 1;
					}

                    else if (p1.getSatisfaction() > p2.getSatisfaction())
                    {
                        return 1;
                    }

                    else if (p1.getSatisfaction() < p2.getSatisfaction())
                    {
                        return -1;
                    }
				
					return 0;
				}				
			});
			
			for (Person p : persons) {
				String regionName = IumPeopleService.getRegionKey(p.getGender(), p.getArea().getRegion());
				
				if (!regions.containsKey(regionName)) {
					regions.put(regionName, new FastByIDMap<Person>());
				}
				
				regions.get(regionName).put(p.getId(), p);				
				
				if (p.getGender() == Gender.MALE) {
					males.put(p.getId(), p);
				} else {
					females.put(p.getId(), p);
				}
			}
			
			
			DataModel dataModel = preferenceService.getDataModel(gender, males);

            WeightedRecommenderPicker picker = new WeightedRecommenderPicker();

//			Algorithm algorithm = algorithmService.getLastAlgorithm();
//
//			if(algorithm == null) {
//				algorithm = new Algorithm();
//				algorithm.setGender(gender);
//				algorithm.setId(1L);
//				algorithm.setNeighborhoodCount(3);
//				algorithm.setRegisted(Calendar.getInstance().getTime());
//				algorithm.setSimilarityAlgorithm(SimilarityAlgorithm.PEARSON_CORRELATION_SIMILARITY);
//	//			algorithm.setTrainingPercentage(90L);
//			}
//
//			UserSimilarity userSimilarity =
//					algorithm.getSimilarityAlgorithm().getUserSimilarity().getDeclaredConstructor(DataModel.class, Weighting.class).
//					newInstance(dataModel, Weighting.WEIGHTED);

            recommenderStatusService.startWork("그룹 선호도 가져오기...", 1);
            FastByIDMap<FastByIDMap<GroupPreference>> groupPreferences = reservedMatchConditionService.getGroupPreferenceAll(reservedMatchCondition.getSeqId());
            recommenderStatusService.addCurrent();

            IumRecommender r;

            // 1. baseline
            UserSimilarity sim1 = new PearsonCorrelationSimilarity(dataModel, Weighting.UNWEIGHTED);
            UserNeighborhood nei1 = new NearestNUserNeighborhood(100, sim1, dataModel);
            r = new IumUserBasedRecommender(dataModel, nei1, sim1, males, females, groupPreferences);
            r.setDescription("UCF-P-NN");

//            picker.add(r, 5);

            // 2. LL
            UserSimilarity sim2 = new LogLikelihoodSimilarity(dataModel);
            UserNeighborhood nei2 = new NearestNUserNeighborhood(20, sim2, dataModel);
            r = new IumUserBasedRecommender(dataModel, nei2, sim2, males, females, groupPreferences);
            r.setDescription("UCF-LL-NN");

            picker.add(r, 5);

			// 메인
			Map<MatchKey, Match> matches = new HashMap<MatchKey, Match>();
			
			String[] regions = { "수도권", "전라권", "경북권", "경남권", "충청권" };
			
			for (String region : regions) {
				matches.putAll(getMatches(gender, region, dataModel, picker, groupPreferences));
			}

			// 쩌리들
			FastByIDMap<Person> extraMales = new FastByIDMap<>();
			FastByIDMap<Person> extraFemales = new FastByIDMap<>();
			
			LongPrimitiveIterator iterator = males.keySetIterator();

			while (iterator.hasNext()) {
				Person male = males.get(iterator.next());
				
				if(male.getCurrentPartners().size() == 0) {
					extraMales.put(male.getId(), male);
				}
			}
			
			matches.putAll(recommenderService.matchExtra(Gender.MALE, "남자쩌리", dataModel, nei1, sim1,
					extraMales, females, groupPreferences));				
			
			iterator = females.keySetIterator();
			
			while (iterator.hasNext()) {
				Person female = females.get(iterator.next());
				
				if(female.getCurrentPartners().size() == 0) {
					extraFemales.put(female.getId(), female);
				}
			}			
			
			matches.putAll(recommenderService.matchExtra(Gender.FEMALE, "여자쩌리", dataModel, nei1, sim1,
					males, extraFemales, groupPreferences));		
					
			// 데이터 보정. 남자들중 2명이상 이음이 들어간 사람 조사해서 매칭 제거
			clearMales(males, females, matches);
			
			iterator = males.keySetIterator();
			
			int totalNotMatchedMales = 0;
			int totalNotMatchedFemles = 0;
            int totalMales = 0 ;
            int totalFemales = 0 ;
			
			while (iterator.hasNext()) {
				Person male = males.get(iterator.next());
				
				if (male.getCurrentPartners().size() == 0) {
					totalNotMatchedMales++;
					logger.debug("not matching male : " + male);
				}
                totalMales++ ;
			}
			
			iterator = females.keySetIterator();
			
			while (iterator.hasNext()) {
				Person female = females.get(iterator.next());
				
				if (female.getCurrentPartners().size() == 0) {
					totalNotMatchedFemles++;
					logger.debug("not matching female : " + female);
				}
                totalFemales++ ;
			}
			
			iumMatchWriter.writeAll(matchDate, matches);
            String resultString = String.format("매칭수:%d 남성수:%d/%d 여성수:%d/%d", matches.size(), totalMales - totalNotMatchedMales, totalMales, totalFemales - totalNotMatchedFemles, totalFemales) ;
			
			recommenderStatusService.stop();

            logger.info("데이터 정합성 체크중");
			
			if(!iumPeopleService.checkData(matchDate, startDate)) {
				messageService.sendSms(phones, "[콘체]오류발생 (중복or매칭안됨) - " + resultString);
                logger.error("[콘체]오류발생 (중복or매칭안됨) - " + resultString);

                return;
			}
		
			double duration = (System.nanoTime() - startTime)/1000000.0/1000/60;		
			messageService.sendSms(phones, "[콘체]매칭정상종료\n걸린시간: " + String.format("%.2f", duration) + "분 - " + resultString);
            logger.info("[콘체]매칭정상종료\n걸린시간: " + String.format("%.2f", duration) + "분 - " + resultString);
			logger.info( "TIME : " + duration + "(min)" );			
		} catch(Exception e) {
			messageService.sendSms(phones, "[콘체]매칭 오류");
			logger.debug(e.getMessage(), e);
		}
        System.gc() ;
	}
	
	
	public void clearMales(FastByIDMap<Person> males, FastByIDMap<Person> females, 
			Map<MatchKey, Match> matches) {
		LongPrimitiveIterator iterator = males.keySetIterator();
		recommenderStatusService.startWork("전체 정리하는중 ...", males.size());
		System.out.println("전체 정리하는중 ...");
		Person me = null;
		Person partner = null;
		int count = 0;
        int i = 0;
		while (iterator.hasNext()) {		
			me = males.get(iterator.next());
			 
			count = 0;
			while(1 < me.getCurrentPartners().size() && count < 4) {
				Long partnerId = me.getCurrentPartners().get(0);
				partner = females.get(partnerId);

				if (partner != null && partner.getCurrentPartners() != null && 1 < partner.getCurrentPartners().size()) {
					//System.out.println(me + " " + partner);
					me.getCurrentPartners().remove(partnerId);
					partner.getCurrentPartners().remove(me.getId());
					matches.remove(new MatchKey(me.getId(), partnerId));
                    i++;
				}
				
				count++;
			}
			
			recommenderStatusService.addCurrent();
		}

        logger.info(i + " matches removed");
	}

    private Map<MatchKey, Match> getMatches(Gender gender, String regionName, DataModel dataModel,
                                            WeightedRecommenderPicker picker,
                                            FastByIDMap<FastByIDMap<GroupPreference>> groupPreferences)
            throws TasteException {
        FastByIDMap<Person> males = regions.get(IumPeopleService.getRegionKey(Gender.MALE, regionName));
        FastByIDMap<Person> females = regions.get(IumPeopleService.getRegionKey(Gender.FEMALE, regionName));
        Map<MatchKey, Match> matches = recommenderService.match(gender, regionName, dataModel, picker,
                males, females, groupPreferences);
        return matches;
    }

	public RecommenderStatus getRecommenderStatus() {
		return recommenderStatusService.getRecommenderStatus();
	}	
	
	public boolean checkData(String matchDate) {
		matchDate += " 12:30:00";
		return iumPeopleService.checkData(matchDate, matchDate);
	}

    private Properties getAppProperties() {
        Properties properties = new Properties();

        try {
            properties.load(getClass().getResourceAsStream("/app.properties"));
        } catch (IOException ex) {
            Logger.getLogger(WebAppInitializer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return properties;
    }
}
