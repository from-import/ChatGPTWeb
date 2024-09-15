package com.fromimport.chatgptweb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {
    /**
     * PriorityBlockingQueue 是一个无界的、基于优先级的队列，队列中的任务按照优先级进行排序，而不是按照插入的顺序执行。优先级的高低由任务的比较方式决定
     * 使用 PriorityBlockingQueue 确定任务优先级的步骤：
     *
     *     定义任务类：要使用优先级队列，你的任务类需要实现 Comparable 接口，并在 compareTo() 方法中定义优先级的比较逻辑。
     *     使用 PriorityBlockingQueue：将任务放入 PriorityBlockingQueue，任务会自动按照优先级排序。
     *     线程池执行：通过线程池执行这些带有优先级的任务。
     *     import java.util.concurrent.PriorityBlockingQueue;
     *
     * public class PriorityTask implements Comparable<PriorityTask> {
     *     private String name;
     *     private int priority; // 优先级，数字越小优先级越高
     *
     *     public PriorityTask(String name, int priority) {
     *         this.name = name;
     *         this.priority = priority;
     *     }
     *
     *     @Override
     *     public int compareTo(PriorityTask other) {
     *         // 比较优先级，优先级数字越小越优先
     *         return Integer.compare(this.priority, other.priority);
     *     }
     *
     *     public void execute() {
     *         System.out.println("Executing task: " + name + " with priority: " + priority);
     *     }
     *
     *     @Override
     *     public String toString() {
     *         return "Task{name='" + name + "', priority=" + priority + '}';
     *     }
     * }
     * @return
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);   // 核心线程数
        executor.setMaxPoolSize(10);   // 最大线程数
        executor.setQueueCapacity(100); // 队列容量
        executor.setThreadNamePrefix("ChatTask-"); // 线程名前缀
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 当线程池和队列已满时，新任务将由提交任务的线程自己执行
        executor.initialize();
        return executor;
    }
}
