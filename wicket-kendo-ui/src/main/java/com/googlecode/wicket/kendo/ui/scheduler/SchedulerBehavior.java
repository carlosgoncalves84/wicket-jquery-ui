/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.wicket.kendo.ui.scheduler;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.CallbackParameter;

import com.googlecode.wicket.jquery.core.JQueryEvent;
import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.core.ajax.IJQueryAjaxAware;
import com.googlecode.wicket.jquery.core.ajax.JQueryAjaxBehavior;
import com.googlecode.wicket.jquery.core.utils.RequestCycleUtils;
import com.googlecode.wicket.kendo.ui.KendoUIBehavior;
import com.googlecode.wicket.kendo.ui.scheduler.views.SchedulerViewType;

/**
 * Provides the Kendo UI scheduler behavior
 *
 * @author Sebastien Briquet - sebfz1
 *
 */
public abstract class SchedulerBehavior extends KendoUIBehavior implements IJQueryAjaxAware, ISchedulerListener
{
	private static final long serialVersionUID = 1L;

	static final String METHOD = "kendoScheduler";

	//private JQueryAjaxBehavior onAddBehavior = null;
	private JQueryAjaxBehavior onCreateBehavior = null;
	private JQueryAjaxBehavior onEditBehavior = null;
	private JQueryAjaxBehavior onUpdateBehavior = null;
	private JQueryAjaxBehavior onDeleteBehavior = null;

	/**
	 * Constructor
	 *
	 * @param selector the html selector (ie: "#myId")
	 */
	public SchedulerBehavior(final String selector)
	{
		this(selector, new Options());
	}

	/**
	 * Constructor
	 *
	 * @param selector the html selector (ie: "#myId")
	 * @param options the {@link Options}
	 */
	public SchedulerBehavior(final String selector, Options options)
	{
		super(selector, METHOD, options);
	}

	// Methods //

	@Override
	public void bind(Component component)
	{
		super.bind(component);

		// events //
		//component.add(this.onAddBehavior 	= this.newOnAddBehavior());
		component.add(this.onCreateBehavior = this.newOnCreateBehavior());
		if (this.isEditEnabled())
		{
			component.add(this.onEditBehavior 	= this.newOnEditBehavior());
		}
		component.add(this.onUpdateBehavior = this.newOnUpdateBehavior());
		component.add(this.onDeleteBehavior = this.newOnDeleteBehavior());
	}

	// Properties //

	protected abstract CharSequence getDataSourceUrl();

	// Events //

	@Override
	public void onConfigure(Component component)
	{
		super.onConfigure(component);

		// data-sources //
		final SchedulerDataSource dataSource = this.newSchedulerDataSource("schedulerDataSource");
		this.add(dataSource);

		// options //
		this.setOption("autoBind", true);
		// this.setOption("autoSync", true); // client side, probably useless

		// events //
		//this.setOption("add", this.onAddBehavior.getCallbackFunction());
		if(this.isEditEnabled())
		{
			this.setOption("edit", this.onEditBehavior.getCallbackFunction());
		}
		// this.setOption("save", "function(e) { console.log('save'); console.log(e); }");
		// this.setOption("change", "function(e) { console.log('change'); console.log(e); }");
		// this.setOption("remove", "function(e) { console.log('remove'); console.log(e); }");

		// data source //
		this.setOption("dataSource", dataSource.getName());
	}

	@Override
	public void onAjax(AjaxRequestTarget target, JQueryEvent event)
	{
		SchedulerEvent e = (SchedulerEvent) event;

		//		if (event instanceof AddEvent)
//		{
//			this.onAdd(target, e.getStart(), e.getEnd(), e.getAllDay());
//		}

		if (event instanceof CreateEvent)
		{
			this.onCreate(target, e);
		}

		if (event instanceof EditEvent)
		{
			this.onEdit(target, e, e.getView());
		}

		if (event instanceof UpdateEvent)
		{
			this.onUpdate(target, e);
		}

		if (event instanceof DeleteEvent)
		{
			this.onDelete(target, e);
		}
	}

	// Factories //

	private SchedulerDataSource newSchedulerDataSource(String name)
	{
		SchedulerDataSource ds = new SchedulerDataSource(name);

		ds.setTransportRead(this.getReadCallbackFunction());
		ds.setTransportCreate(this.onCreateBehavior.getCallbackFunction());
		ds.setTransportUpdate(this.onUpdateBehavior.getCallbackFunction());
		ds.setTransportDelete(this.onDeleteBehavior.getCallbackFunction());

		return ds;
	}

	/**
	 * As create, update and destroy need to be supplied, we should declare read as a function. Weird...
	 *
	 * @return the 'read' callback function
	 */
	private String getReadCallbackFunction()
	{
		String widget = this.widget(METHOD);
		String start = widget + ".view().startDate().getTime()";
		String end = widget + ".view().endDate().getTime()";

		return "function(options) {" // lf
				+ "	jQuery.ajax({" // lf
				+ "		url: '" + this.getDataSourceUrl() + "'," // lf
				+ "		data: { start: " + start + ",  end: " + end + "}, " // lf
				+ "		dataType: 'json'," // lf
				+ "		success: function(result) {" // lf
				+ "			options.success(result);" // lf
				+ "		}," // lf
				+ "		error: function(result) {" // lf
				+ "			options.error(result);" // lf
				+ "		}" // lf
				+ "	});" // lf
				+ "}";
	}

//	protected JQueryAjaxBehavior newOnAddBehavior()
//	{
//		return new JQueryAjaxBehavior(this) {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected JQueryEvent newEvent()
//			{
//				return new AddEvent();
//			}
//			
//			@Override
//			protected CallbackParameter[] getCallbackParameters()
//			{
//				return new CallbackParameter[] { CallbackParameter.context("e"),
//												 CallbackParameter.resolved("start", "e.event.start.getTime()"),
//												 CallbackParameter.resolved("end", "e.event.end.getTime()"),
//												 CallbackParameter.resolved("allday", "e.event.isAllDay") };
//			}
//
//			@Override
//			public CharSequence getCallbackFunctionBody(CallbackParameter... extraParameters)
//			{
//				return super.getCallbackFunctionBody(extraParameters) + " e.preventDefault();";//avoid propagation of KendoUIs edit-event after add-event
//			}
//
//		};
//	}

	protected JQueryAjaxBehavior newOnCreateBehavior()
	{
		return new CallbackAjaxBehavior(this) {

			private static final long serialVersionUID = 1L;

			@Override
			protected JQueryEvent newEvent()
			{
				return new CreateEvent();
			}
		};
	}

	protected JQueryAjaxBehavior newOnEditBehavior()
	{
		return new JQueryAjaxBehavior(this) {//TODO check ***

			private static final long serialVersionUID = 1L;

			@Override
			protected JQueryEvent newEvent()
			{
				return new EditEvent();
			}
			
			@Override
			protected CallbackParameter[] getCallbackParameters()
			{
				return new CallbackParameter[] { CallbackParameter.context("e"),
												 CallbackParameter.resolved("id", "e.event.id"),
												 CallbackParameter.resolved("start", "e.event.start.getTime()"),
												 CallbackParameter.resolved("end", "e.event.end.getTime()"),
												 CallbackParameter.resolved("view", "e.sender.view().name")};
			}

			@Override
			public CharSequence getCallbackFunctionBody(CallbackParameter... extraParameters)
			{
				return super.getCallbackFunctionBody(extraParameters) + " e.preventDefault();";//avoid propagation of KendoUIs edit-event on client-side
			}

		};
	}

	protected JQueryAjaxBehavior newOnUpdateBehavior()
	{
		return new CallbackAjaxBehavior(this) {

			private static final long serialVersionUID = 1L;

			@Override
			protected JQueryEvent newEvent()
			{
				return new UpdateEvent();
			}
		};
	}

	protected JQueryAjaxBehavior newOnDeleteBehavior()
	{
		return new CallbackAjaxBehavior(this) {

			private static final long serialVersionUID = 1L;

			@Override
			protected JQueryEvent newEvent()
			{
				return new DeleteEvent();
			}
		};
	}

	// Event classes //

	abstract static class CallbackAjaxBehavior extends JQueryAjaxBehavior
	{
		private static final long serialVersionUID = 1L;

		public CallbackAjaxBehavior(IJQueryAjaxAware source)
		{
			super(source);
		}

		@Override
		protected CallbackParameter[] getCallbackParameters()
		{
			return new CallbackParameter[] { CallbackParameter.context("options"), // lf
					CallbackParameter.resolved("id", "options.data.id"), // retrieved
					CallbackParameter.resolved("start", "options.data.start.getTime()"), // retrieved
					CallbackParameter.resolved("end", "options.data.end.getTime()"), // retrieved
					CallbackParameter.resolved("title", "options.data.title") // retrieved
			};
		}

		@Override
		public CharSequence getCallbackFunctionBody(CallbackParameter... extraParameters)
		{
			return super.getCallbackFunctionBody(extraParameters) + " options.success();";
		}
	}

	protected static class CallbackSchedulerEvent extends SchedulerEvent
	{
		private static final long serialVersionUID = 1L;

		public CallbackSchedulerEvent()
		{
			int id = RequestCycleUtils.getQueryParameterValue("id").toInt();
			this.setId(id);

			String title = RequestCycleUtils.getQueryParameterValue("title").toString();
			this.setTitle(title);

			long start = RequestCycleUtils.getQueryParameterValue("start").toLong();
			this.setStart(start);

			long end = RequestCycleUtils.getQueryParameterValue("end").toLong();
			this.setEnd(end);
			
			//TODO note: this is scheduler-event related, e.g. edit
			String view = RequestCycleUtils.getQueryParameterValue("view").toString();
			this.setView(SchedulerViewType.get(view));
		}
	}

//	protected static class AddEvent extends JQueryEvent
//	{
//		private final Date start;
//		private final Date end;
//		private final boolean allDay;
//		
//		public AddEvent()
//		{
//			long start = RequestCycleUtils.getQueryParameterValue("start").toLong();
//			this.start = new Date(start);
//
//			long end = RequestCycleUtils.getQueryParameterValue("end").toLong();
//			this.end = new Date(end);
//			
//			this.allDay = RequestCycleUtils.getQueryParameterValue("allday").toBoolean();
//		}
//		
//		public Date getStart() {
//			return start;
//		}
//		
//		public Date getEnd() {
//			return end;
//		}
//		
//		public boolean getAllDay() {
//			return allDay;
//		}
//	}

	protected static class CreateEvent extends CallbackSchedulerEvent
	{
		private static final long serialVersionUID = 1L;
	}

	//TODO *** is it ok, to inherit parameter-reading features from CallbackSchedulerEvent,
	//but not to extend the newOnEditBehavior from CallbackAjaxBehavior and provide own CallBackParameter definitions?
	//see also *** of newOnEditEventBehavior()
	//I am not sure, if I understood CallbackAjaxBehavior right... it looks like its more datasource related, instead of calender related?!
	//It is possible I accidently mixed it up.
	protected static class EditEvent extends CallbackSchedulerEvent
	{
		private static final long serialVersionUID = 1L;
	}

	protected static class UpdateEvent extends CallbackSchedulerEvent
	{
		private static final long serialVersionUID = 1L;
	}

	protected static class DeleteEvent extends CallbackSchedulerEvent
	{
		private static final long serialVersionUID = 1L;
	}
}
