package com.rabbitmq.socks.test.stress;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.rabbitmq.socks.api.RabbitSocksAPI;
import com.rabbitmq.socks.api.RabbitSocksAPIFactory;
import com.rabbitmq.socks.test.APITestBase;

/**
 *
 * @author tfox
 *
 */
public class StressTest extends APITestBase
{
	private final String channelPub = "ch-pub";
    private final String channelSub = "ch-sub";
    private final String channelPush = "ch-push";
    private final String channelPull = "ch-pull";
    private final String channelReq = "ch-req";
    private final String channelRep = "ch-rep";
    
    private final int numMessages = 10;
    private long runLength;
    
    private volatile ExecutorService exec;
    private volatile RabbitSocksAPI api;
    
    @Override
    protected void setUp() throws Exception
    {
    	exec = Executors.newCachedThreadPool();    
    	String host = System.getProperty("rsa.host", "localhost");
    	String port = System.getProperty("rsa.port", "55672");
    	runLength = Integer.valueOf(System.getProperty("rsa.runlength", "1800000")).intValue();
    	api = RabbitSocksAPIFactory.getClient(host, Integer.valueOf(port).intValue(), "socks-api/default",
                "guest", "guest");
    	System.out.println("Using host: " + host + " port: " + port + " runlength: " + runLength);
    }
    
    @Override
    protected void tearDown() throws Exception
    {
    	exec.shutdown();
    }
    
    public void _testOpenCloseRapidly() throws Exception
    {
        WorkerFactory wf = createOpenCloseWorkerFactory();
        Worker[] workers = createWorkers(wf, 30);
        runWorkers(workers);        
    }

    public void _testSimplePubSub() throws Exception
    {
        WorkerFactory wf = createPubSubWorkerFactory();
        Worker[] workers = createWorkers(wf, 30);
        runWorkers(workers);
    }

    public void _testSimplePushPull() throws Exception
    {
        WorkerFactory wf = createPushPullWorkerFactory();
        Worker[] workers = createWorkers(wf, 30);
        runWorkers(workers);
    }

    public void _testSimpleRequestResponse() throws Exception
    {
        WorkerFactory wf = createReqRepWorkerFactory();
        Worker[] workers = createWorkers(wf, 30);
        runWorkers(workers);
    }
       
    public void testAllTogether() throws Exception
    {
    	System.out.println("Running for " + runLength + " ms");
        WorkerFactory wfOpenClose = createOpenCloseWorkerFactory();
        WorkerFactory wfPubSub = createPubSubWorkerFactory();
        WorkerFactory wfPushPull = createPushPullWorkerFactory();
        WorkerFactory wfReqRep = createReqRepWorkerFactory();
        
        Worker[] wf1 = createWorkers(wfOpenClose, 10);
        Worker[] wf2 = createWorkers(wfPubSub, 10);
        Worker[] wf3 = createWorkers(wfPushPull, 10);
        Worker[] wf4 = createWorkers(wfReqRep, 10);
        
        Worker[] allWorkers = addWorkers(wf1, addWorkers(wf2, addWorkers(wf3, wf4)));
        
        runWorkers(allWorkers);
    }

    private interface WorkerFactory
    {
        Worker createWorker();
    }
    
    private Worker[] addWorkers(Worker[] w1, Worker[] w2)
    {
    	List<Worker> workers = new ArrayList<Worker>();
    	workers.addAll(java.util.Arrays.asList(w1));
    	workers.addAll(java.util.Arrays.asList(w2));
    	return (Worker[])workers.toArray(new Worker[0]);
    }
    
    private Worker[] createWorkers(final WorkerFactory wf, final int numWorkers)
    {
    	Worker[] workers = new Worker[numWorkers];
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = wf.createWorker();
        }
        return workers;
    }
    
    private void runWorkers(Worker[] workers) throws Exception
	{
	    for (int i = 0; i < workers.length; i++)
	    {
	        workers[i].start();
	    }
	    for (int i = 0; i < workers.length; i++)
	    {
	        workers[i].join();
	        Exception e = workers[i].getException();
	        if (e != null)
	        {
	        	String msg = "Got exception from worker: " + e.getMessage();
	        	System.err.println(msg);
	        	e.printStackTrace(System.err);
	        	fail(msg);
	        }
	        System.out.println("Worker: " + workers[i] + " did count " + workers[i].getCount());
	    }
	    System.out.println("Workers stopped");
	}
    
    private WorkerFactory createOpenCloseWorkerFactory()
    {
    	return new WorkerFactory()
        {
            @Override
            public Worker createWorker()
            {
                return new OpenCloseWorker(api, exec, runLength);
            }
        };              
    }
        
    private WorkerFactory createPubSubWorkerFactory()
    {
    	return new WorkerFactory()
        {
            int i;
            @Override
            public Worker createWorker()
            {
                return new PubSubWorker(api, exec, "topic-" + i++,
                                        channelPub, channelSub, numMessages, runLength);
            }
        };        
    }
    
    private WorkerFactory createPushPullWorkerFactory()
    {
    	return new WorkerFactory()
        {
            int i;
            @Override
            public Worker createWorker()
            {
                return new PushPullWorker(api, exec, "queue-" + i++,
                                        channelPush, channelPull, numMessages,
                                        runLength);
            }
        };
    }
    
    private WorkerFactory createReqRepWorkerFactory()
    {
    	return new WorkerFactory()
        {
            int i;
            @Override
            public Worker createWorker()
            {
                return new ReqRepWorker(api, exec, "conv-" + i++,
                                        channelReq, channelRep, numMessages,
                                        runLength);
            }
        };
    }
    
}
