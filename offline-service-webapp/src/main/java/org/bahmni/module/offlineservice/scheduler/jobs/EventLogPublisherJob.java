package org.bahmni.module.offlineservice.scheduler.jobs;

import org.apache.log4j.Logger;
import org.bahmni.module.offlineservice.mapper.EventRecordsToEventLogMapper;
import org.bahmni.module.offlineservice.model.EventRecords;
import org.bahmni.module.offlineservice.model.EventLog;
import org.bahmni.module.offlineservice.repository.EventRecordsRepository;
import org.bahmni.module.offlineservice.repository.EventLogRepository;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

@DisallowConcurrentExecution
@Component("eventLogPublisherJob")
@ConditionalOnExpression("'${enable.scheduling}'=='true'")
public class EventLogPublisherJob implements Job {
    private EventRecordsRepository eventRecordsRepository;
    private EventLogRepository eventLogRepository;
    private EventRecordsToEventLogMapper eventRecordsToEventLogMapper;

    private static Logger logger = Logger.getLogger(EventLogPublisherJob.class);

    @Autowired
    public EventLogPublisherJob(EventRecordsRepository eventRecordsRepository, EventLogRepository eventLogRepository, EventRecordsToEventLogMapper eventRecordsToEventLogMapper) {
        this.eventRecordsRepository = eventRecordsRepository;
        this.eventLogRepository = eventLogRepository;
        this.eventRecordsToEventLogMapper = eventRecordsToEventLogMapper;
    }

    @Override
    public void process() throws InterruptedException {
        EventLog eventLog = eventLogRepository.findFirstByOrderByTimestampDesc();
        List<EventRecords> eventRecords;

        if (eventLog != null) {
            logger.debug("Reading events which are happened after : " + eventLog.getTimestamp().toString());
            eventRecords = eventRecordsRepository.findAllEventsAfterTimestamp(eventLog.getTimestamp());
        } else {
            logger.debug("Reading all events from event_records");
            eventRecords = eventRecordsRepository.findAll();
        }

        logger.debug("Found " + eventRecords.size() + " events.");
        List<EventLog> eventLogs = eventRecordsToEventLogMapper.map(eventRecords);
        eventLogRepository.save(eventLogs);
        logger.debug("Copied " + eventRecords.size() + " events to events_log table");
    }
}
