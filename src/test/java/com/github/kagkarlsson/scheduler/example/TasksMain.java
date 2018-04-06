package com.github.kagkarlsson.scheduler.example;

import com.github.kagkarlsson.scheduler.HsqlTestDatabaseRule;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.task.*;
import com.github.kagkarlsson.scheduler.task.helper.ComposableTask;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

import static java.time.Duration.ofHours;

public class TasksMain {
	private static final Logger LOG = LoggerFactory.getLogger(TasksMain.class);

	public static void main(String[] args) throws Throwable {
		try {
			final HsqlTestDatabaseRule hsqlRule = new HsqlTestDatabaseRule();
			hsqlRule.before();
			final DataSource dataSource = hsqlRule.getDataSource();

//			recurringTask(dataSource);
//			adhocTask(dataSource);
			simplerTaskDefinition(dataSource);
		} catch (Exception e) {
			LOG.error("Error", e);
		}
	}

	private static void recurringTask(DataSource dataSource) {

		RecurringTask<Void> hourlyTask = ComposableTask.recurringTask(
				"my-hourly-task",
				FixedDelay.of(ofHours(1)),
				(inst, ctx) -> System.out.println("Executed!"));

		final Scheduler scheduler = Scheduler
				.create(dataSource)
				.startTasks(hourlyTask)
				.threads(5)
				.build();

		// hourlyTask is automatically scheduled on startup if not already started (i.e. exists in the db)
		scheduler.start();
	}

	public static class MyHourlyTask extends RecurringTask<Void> {

		public MyHourlyTask() {
			super("my-hourly-task", FixedDelay.of(ofHours(1)), Void.class, null);
		}

		@Override
		public void executeRecurringly(TaskInstance<Void> taskInstance, ExecutionContext executionContext) {
			System.out.println("Executed!");
		}
	}

	private static void adhocTask(DataSource dataSource) {

		final MyTypedAdhocTask myAdhocTask = new MyTypedAdhocTask();

		final Scheduler scheduler = Scheduler
				.create(dataSource, myAdhocTask)
				.threads(5)
				.build();

		scheduler.start();

		// Schedule the task for execution a certain time in the future and optionally provide custom data for the execution
		scheduler.schedule(myAdhocTask.instance("1045", new MyTaskData(1001L, "custom-data")), Instant.now().plusSeconds(5));
	}

	public static class MyTaskData implements Serializable {
		public final long id;
		public final String secondaryId;

		public MyTaskData(long id, String secondaryId) {
			this.id = id;
			this.secondaryId = secondaryId;
		}
	}

	public static class MyTypedAdhocTask extends OneTimeTask<MyTaskData> {

		public MyTypedAdhocTask() {
			super("my-typed-adhoc-task", MyTaskData.class);
		}

		@Override
		public void executeOnce(TaskInstance<MyTaskData> taskInstance, ExecutionContext executionContext) {
			System.out.println(String.format("Executed! Custom data: [Id: %s], [secondary-id: %s]", taskInstance.getData().id, taskInstance.getData().secondaryId));
		}
	}

	private static void simplerTaskDefinition(DataSource dataSource) {

		final RecurringTask<Void> myHourlyTask = ComposableTask.recurringTask("my-hourly-task", FixedDelay.of(ofHours(1)),
				(inst, ctx) -> System.out.println("Executed!"));

		final OneTimeTask<Void> oneTimeTask = ComposableTask.onetimeTask("my-onetime-task", Void.class,
				(inst, ctx) -> System.out.println("One-time task with id "+inst.getId()+" executed!"));

		final Scheduler scheduler = Scheduler
				.create(dataSource, oneTimeTask)
				.startTasks(myHourlyTask)
				.threads(5)
				.build();

		scheduler.start();


		scheduler.schedule(oneTimeTask.instance("1001"), Instant.now().plus(Duration.ofSeconds(5)));
	}

}
