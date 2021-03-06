package cn.com.warlock.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.com.warlock.common.util.NodeNameHolder;
import cn.com.warlock.scheduler.helper.ConsistencyHash;
import cn.com.warlock.scheduler.registry.NullJobRegistry;

public class JobContext {

	private static JobContext context = new JobContext();
	
	private Set<String> activeNodes = new HashSet<String>();
	
	private ConsistencyHash hash = new ConsistencyHash();
	
	private Map<String, AbstractJob> allJobs = new HashMap<>();
	
	private ConfigPersistHandler configPersistHandler;
	
	private TaskRetryProcessor retryProcessor;
	
	private JobLogPersistHandler jobLogPersistHandler;
	
	private JobRegistry registry;
	
	public void startRetryProcessor(){
		if(retryProcessor == null){
			synchronized (context) {
				if(retryProcessor != null)return;
				retryProcessor = new TaskRetryProcessor(1);
			}
		}
	}

	public static JobContext getContext() {
		return context;
	}

	public String getNodeId() {
		return NodeNameHolder.getNodeId();
	}
	
	public ConfigPersistHandler getConfigPersistHandler() {
		return configPersistHandler;
	}

	public void setConfigPersistHandler(ConfigPersistHandler configPersistHandler) {
		this.configPersistHandler = configPersistHandler;
	}

	public JobLogPersistHandler getJobLogPersistHandler() {
		return jobLogPersistHandler;
	}

	public void setJobLogPersistHandler(JobLogPersistHandler jobLogPersistHandler) {
		this.jobLogPersistHandler = jobLogPersistHandler;
	}

	public JobRegistry getRegistry() {
		if(registry == null){
			registry = new NullJobRegistry();
		}
		return registry;
	}

	public void setRegistry(JobRegistry registry) {
		this.registry = registry;
	}

	public TaskRetryProcessor getRetryProcessor() {
		return retryProcessor;
	}

	public void refreshNodes(List<String> nodes){
		activeNodes.clear();
		activeNodes.addAll(nodes);
		if(activeNodes.isEmpty())return;
		hash.refresh(nodes);
	}
	
	public void addNode(String node){
		activeNodes.add(node);
		hash.refresh(new ArrayList<>(activeNodes));
	}
	
	public void removeNode(String node){
		activeNodes.remove(node);
		if(activeNodes.isEmpty())return;
		hash.refresh(new ArrayList<>(activeNodes));
	}
	
	public boolean matchCurrentNode(Object shardFactor){
		if(activeNodes.size() == 1)return true;
		String expectNodeId = hash.getAssignedRealNode(shardFactor);
		return expectNodeId.equals(getNodeId());
	}
	
	public void addJob(AbstractJob job){
		String key = job.group + ":" + job.jobName;
		allJobs.put(key, job);
	}

	public Map<String, AbstractJob> getAllJobs() {
		return allJobs;
	}

	public Set<String> getActiveNodes() {
		return activeNodes;
	}
	
	public void close(){
		if(retryProcessor != null){
			retryProcessor.close();
		}
	}

}
