package it.finanze.sanita.fse2.ms.gtw.dispatcher.service.impl;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.client.IConfigClient;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ConfigItemDTO;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfigItemTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.service.IConfigSRV;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.client.routes.base.ClientRoutes.Config.*;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ConfigItemDTO.ConfigDataItemDTO;
import static it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.ConfigItemTypeEnum.DISPATCHER;

@Slf4j
@Service
public class ConfigSRV implements IConfigSRV {

    private static final long DELTA_MS = 300_000L;

    @Autowired
    private IConfigClient client;

	private final Map<String, Pair<Long, String>> props;

	public ConfigSRV() {
		this.props = new HashMap<>();
	}

    
    @PostConstruct
    public void postConstruct() {
        for(ConfigItemTypeEnum en : ConfigItemTypeEnum.priority()) {
            log.info("[GTW-CFG] Retrieving {} properties ...", en.name());
            ConfigItemDTO items = client.getConfigurationItems(en);
            List<ConfigDataItemDTO> opts = items.getConfigurationItems();
            for(ConfigDataItemDTO opt : opts) {
                opt.getItems().forEach((key, value) -> {
                    log.info("[GTW-CFG] Property {} is set as {}", key, value);
                    props.put(key, Pair.of(new Date().getTime(), value));
                });
                if(opt.getItems().isEmpty()) log.info("[GTW-CFG] No props were found");
            }
        }
        integrity();
    }

    @Override
    public Boolean isAuditEnable() {
        long lastUpdate = props.get(PROPS_NAME_AUDIT_ENABLED).getKey();
        if (new Date().getTime() - lastUpdate >= DELTA_MS) {
            synchronized(PROPS_NAME_CONTROL_LOG_ENABLED) {
                if (new Date().getTime() - lastUpdate >= DELTA_MS) {
                    refresh(PROPS_NAME_AUDIT_ENABLED);
                }
            }
        }
        return Boolean.parseBoolean(
            props.get(PROPS_NAME_AUDIT_ENABLED).getValue()
        );
    }

    @Override
    public Boolean isControlLogPersistenceEnable() {
        long lastUpdate = props.get(PROPS_NAME_CONTROL_LOG_ENABLED).getKey();
        if (new Date().getTime() - lastUpdate >= DELTA_MS) {
            synchronized(PROPS_NAME_CONTROL_LOG_ENABLED) {
                if (new Date().getTime() - lastUpdate >= DELTA_MS) {
                    refresh(PROPS_NAME_CONTROL_LOG_ENABLED);
                }
            }
        }
        return Boolean.parseBoolean(
            props.get(PROPS_NAME_CONTROL_LOG_ENABLED).getValue()
        );
    }
  
    @Override
	public Boolean isSubjectNotAllowed() {
		long lastUpdate = props.get(PROPS_NAME_SUBJECT).getKey();
		if (new Date().getTime() - lastUpdate >= DELTA_MS) {
			synchronized (PROPS_NAME_SUBJECT) {
				if (new Date().getTime() - lastUpdate >= DELTA_MS) {
					refresh(PROPS_NAME_SUBJECT);
				}
			}
		}
		return Boolean.parseBoolean(
			props.get(PROPS_NAME_SUBJECT).getValue()
		);
	}
    
    @Override
	public Boolean isCfOnIssuerNotAllowed() {
		long lastUpdate = props.get(PROPS_NAME_ISSUER_CF).getKey();
		if (new Date().getTime() - lastUpdate >= DELTA_MS) {
			synchronized(PROPS_NAME_ISSUER_CF) {
				if (new Date().getTime() - lastUpdate >= DELTA_MS) {
					refresh(PROPS_NAME_ISSUER_CF);
				}
			}
		}
		return Boolean.parseBoolean(
			props.get(PROPS_NAME_ISSUER_CF).getValue()
		);
	}

    private void refresh(String name) {
        String previous = props.getOrDefault(name, Pair.of(0L, null)).getValue();
        String prop = client.getProps(name, previous, DISPATCHER);
        props.put(name, Pair.of(new Date().getTime(), prop));
    }

    private void integrity() {
        String err = "Missing props {} from dispatcher";
        String[] out = new String[]{
            PROPS_NAME_AUDIT_ENABLED,
            PROPS_NAME_CONTROL_LOG_ENABLED,
            PROPS_NAME_SUBJECT,
            PROPS_NAME_ISSUER_CF
        };
        for (String prop : out) {
            if(!props.containsKey(prop)) throw new IllegalStateException(err.replace("{}", prop));
        }
    }
}
