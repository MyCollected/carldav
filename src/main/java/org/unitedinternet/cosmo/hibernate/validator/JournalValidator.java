package org.unitedinternet.cosmo.hibernate.validator;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VJournal;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.unitedinternet.cosmo.calendar.util.CalendarUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class JournalValidator implements ConstraintValidator<Journal, Calendar> {
    
    private static final Log LOG = LogFactory.getLog(JournalValidator.class);
    
    private volatile ValidationConfig validationConfig;
    
    public boolean isValid(Calendar value, ConstraintValidatorContext context) {
        Calendar calendar = null;
        ComponentList comps = null;
        try {
            calendar = value;
            
            // validate entire icalendar object
            if (calendar != null) {
                calendar.validate(true);
                // additional check to prevent bad .ics
                CalendarUtils.parseCalendar(calendar.toString());
                
                // make sure we have a VJOURNAL
                comps = calendar.getComponents();
                if(comps==null) {
                    LOG.warn("error validating journal: " + calendar.toString());
                    return false;
                }
            }
            if (comps != null) {
                comps = comps.getComponents(Component.VJOURNAL);
            }
            if(comps==null || comps.size()==0) {
                LOG.warn("error validating journal: " + calendar.toString());
                return false;
            }

            VJournal journal = (VJournal) comps.get(0);
            
            if(journal == null || !PropertyValidator.isJournalValid(journal, validationConfig)) {
                LOG.warn("error validating journal: " + calendar.toString());
                return false;
            }
            
            return true;
            
        } catch(ValidationException ve) {
            LOG.warn("event validation error", ve);
            LOG.warn("error validating journal: " + calendar.toString() );
        } catch(ParserException e) {
            LOG.warn("parse error", e);
            LOG.warn("error parsing event: " + calendar.toString() );
        } catch (IOException | RuntimeException e) {
            LOG.warn("Exception occured while parsing calendar", e);
        }
        
        return false;
    }

    @Override
    public void initialize(Journal constraintAnnotation) {
        if(validationConfig == null){
            synchronized(this){
                validationConfig = new ValidationConfig();
            }
        }
    }
    
    private static final class ValidationConfig {
        
        private static final String ALLOWED_RECURRENCES_FREQUENCIES_KEY = "cosmo.event.validation.allowed.recurrence.frequencies";
        private static final String FREQUENCIES_SEPARATOR = ",";
        
        private static final String SUMMARY_MIN_LENGTH_KEY = "cosmo.event.validation.summary.min.length";
        private static final String SUMMARY_MAX_LENGTH_KEY = "cosmo.event.validation.summary.max.length";
        
        private static final String LOCATION_MIN_LENGTH_KEY = "cosmo.event.validation.location.min.length";
        private static final String LOCATION_MAX_LENGTH_KEY = "cosmo.event.validation.location.max.length";
        
        private static final String DESCRIPTION_MIN_LENGTH_KEY = "cosmo.event.validation.description.min.length";
        private static final String DESCRIPTION_MAX_LENGTH_KEY = "cosmo.event.validation.description.max.length";
        
        private static final String ATTENDEES_MAX_LENGTH_KEY = "cosmo.event.validation.attendees.max.length";
        
        private static final String PROPERTIES_FILE = "application.properties";
        
        private final Set<String> allowedRecurrenceFrequencies = new HashSet<>(5);
        
        private int summaryMinLength;
        private int summaryMaxLength;
        
        private int locationMinLength;
        private int locationMaxLength;
        
        private int descriptionMinLength;
        private int descriptionMaxLength;
        
        private int attendeesMaxSize;
        
        boolean initialized = true;
        
        private ValidationConfig (){
            InputStream is = null;
            
            Properties properties = new Properties();
            try {
                is = new ClassPathResource(PROPERTIES_FILE).getInputStream();
                
                properties.load(is);
                
                summaryMinLength = getIntFromPropsFor(properties, SUMMARY_MIN_LENGTH_KEY);
                summaryMaxLength = getIntFromPropsFor(properties, SUMMARY_MAX_LENGTH_KEY);
                
                locationMinLength = getIntFromPropsFor(properties, LOCATION_MIN_LENGTH_KEY);
                locationMaxLength = getIntFromPropsFor(properties, LOCATION_MAX_LENGTH_KEY);
                
                descriptionMinLength = getIntFromPropsFor(properties, DESCRIPTION_MIN_LENGTH_KEY);
                descriptionMaxLength = getIntFromPropsFor(properties, DESCRIPTION_MAX_LENGTH_KEY);
                
                attendeesMaxSize = getIntFromPropsFor(properties, ATTENDEES_MAX_LENGTH_KEY);
                
                String permittedFrequencies = properties.getProperty(ALLOWED_RECURRENCES_FREQUENCIES_KEY);
                String[] frequencies = permittedFrequencies.split(FREQUENCIES_SEPARATOR);
                
                for(String s : frequencies){
                    allowedRecurrenceFrequencies.add(s.trim());
                }
                
            } catch (Exception e) {
                LOG.warn("Failed to initialize validation config", e);
                initialized = false;
            }finally{
                if(is != null){
                    try {
                        is.close();
                    } catch (IOException e) {
                        LOG.warn("Exception occured while closing the input stream", e);
                    }
                }
            }
        }
        
        private static int getIntFromPropsFor(Properties properties, String key){
            return Integer.parseInt(properties.getProperty(key)); 
        }
    }

    private enum PropertyValidator{
        SUMMARY(Property.SUMMARY){

            @Override
            protected boolean isValid(VJournal event, ValidationConfig config) {
                
                return isTextPropertyValid(event.getProperty(prop), config.summaryMinLength, config.summaryMaxLength);
            }
            
        },
        DESCRIPTION(Property.DESCRIPTION){

            @Override
            protected boolean isValid(VJournal event, ValidationConfig config) {
                return isTextPropertyValid(event.getProperty(prop), config.descriptionMinLength, config.descriptionMaxLength);
            }
            
        },
        LOCATION(Property.LOCATION){

            @Override
            protected boolean isValid(VJournal event, ValidationConfig config) {
                return isTextPropertyValid(event.getProperty(prop), config.locationMinLength, config.locationMaxLength);
            }
            
        },
        RECURRENCE_RULE(Property.RRULE){

            @Override
            protected boolean isValid(VJournal event, ValidationConfig config) {
                
                @SuppressWarnings("unchecked")
                List<? extends Property> rrules = event.getProperties(prop);
                if(rrules == null){
                    return true;
                }
                for(Property p : rrules){
                    RRule rrule = (RRule)p; 
                    if(! isRRuleValid(rrule, config)){
                        return false;
                    }
                }
                
                return true;
            }
            
            private boolean isRRuleValid(RRule rrule, ValidationConfig config){
                if(rrule == null){
                    return true;
                }
                
                if(rrule.getRecur() == null || rrule.getRecur().getFrequency() == null){
                    return false;
                }
                
                String recurFrequency = rrule.getRecur().getFrequency();
                if(!config.allowedRecurrenceFrequencies.contains(recurFrequency)){
                    return false;
                }
                
                return true;
            }
            
        }, 
        
        ATTENDEES(Property.ATTENDEE){
            @Override
            protected boolean isValid(VJournal event, ValidationConfig config) {
                List<?> attendees = event.getProperties(prop);
                int attendeesSize = attendees == null ? 0 : attendees.size();
                
                return attendeesSize < config.attendeesMaxSize;
            }
            
        };
        private static final String[] PROPERTIES_WITH_TIMEZONES = {Property.DTSTART, Property.DTEND, Property.EXDATE, Property.RDATE, Property.RECURRENCE_ID}; 
        private static final TimeZoneRegistry timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
        
        final String prop;
        
        PropertyValidator(String propertyToValidate){
            this.prop = propertyToValidate;
        }
        
        protected abstract boolean isValid(VJournal event, ValidationConfig config);
        
        private static boolean isTextPropertyValid(Property prop, int minLength, int maxLength){
            
            if(prop == null && minLength == 0){
                return true;
            }else if(prop == null){
                return false;
            }
            String value = prop.getValue();
            int valueSize = value == null ? 0 :  value.length();
            if(valueSize < minLength || valueSize > maxLength){
                return false;
            }
            
            return true;
        }

        private static boolean isJournalValid(VJournal event, ValidationConfig config){
            DtStart startDate = event.getStartDate();
            if(startDate == null ||
                    startDate.getDate() == null){

                return false;
            }


            if(config.initialized){
                for(PropertyValidator validator : values()){
                    if(! validator.isValid(event, config)){
                        return false;
                    }
                }
            }

            return areTimeZoneIdsValid(event);
        }


        private static boolean areTimeZoneIdsValid(VJournal event){
            for(String propertyName : PROPERTIES_WITH_TIMEZONES){
                @SuppressWarnings("unchecked")
                List<Property> props = event.getProperties(propertyName);
                for(Property p : props){
                    if(p != null && p.getParameter(Parameter.TZID) != null){
                        String tzId = p.getParameter(Parameter.TZID).getValue();
                        if(tzId != null && timeZoneRegistry.getTimeZone(tzId) == null){
                            LOG.warn("Unknown TZID [" + tzId + "] for event " + event);
                            return false;
                            
                        }
                    }
                }
            }
            return true;
        }
    }
}