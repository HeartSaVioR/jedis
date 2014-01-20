package redis.clients.jedis;

public abstract class PipelineBlock {
	protected Pipeline pipeline;
	
    public abstract void execute();
    
    public void setPipeline(Pipeline pipeline) {
    	this.pipeline = pipeline;
    }
	
}
