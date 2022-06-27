/*
 * Copyright (c) 2017. SailPoint Technologies, Inc. All rights reserved.
 */

package com.sailpoint.audit.service.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mark.boyle on 4/25/17.
 */
public class ReportDTOTest {

	ReportDTO _reportDTO;

	String _name ="report";
	String _reportName = "audit-report";
	String _taskDefName = "audit-report";
	String _type = "test";
	String _id = "0123456789ABCDEF";
	Map<String, Object> _arguments = new HashMap<>();
	long _date = new Date().getTime();
	long _duration = 23000L;
	String _status = "Success";
	long _rows = 3L;
	boolean _completed = true;

	@Test
	public void constructorEmptyTest() {

		_reportDTO = new ReportDTO();
		Assert.assertNotNull(_reportDTO);
		Assert.assertNull(_reportDTO.getName());
		Assert.assertNull(_reportDTO.getReportName());
		Assert.assertNull(_reportDTO.getTaskDefName());
		Assert.assertNull(_reportDTO.getType());
		Assert.assertNull(_reportDTO.getId());

		Assert.assertNull(_reportDTO.getArguments());
		Assert.assertNull(_reportDTO.getStatus());

		Assert.assertEquals(0, _reportDTO.getDate());
		Assert.assertEquals(0, _reportDTO.getDuration());
		Assert.assertEquals(0, _reportDTO.getRows());

		Assert.assertFalse(_reportDTO.isCompleted());
	}

	@Test
	public void settersTest() {

		_reportDTO = new ReportDTO();
		_reportDTO.setName(_name);
		_reportDTO.setTaskDefName(_taskDefName);
		_reportDTO.setReportName(_reportName);
		_reportDTO.setId(_id);
		_reportDTO.setType(_type);

		_arguments.put("FOO", "BAR");
		_reportDTO.setArguments(_arguments);
		_reportDTO.setStatus(_status);

		_reportDTO.setDate(_date);
		_reportDTO.setDuration(_duration);
		_reportDTO.setRows(_rows);
		_reportDTO.setCompleted(_completed);

		Assert.assertEquals(_reportName, _reportDTO.getReportName());
		Assert.assertEquals(_name, _reportDTO.getName());
		Assert.assertEquals(_taskDefName, _reportDTO.getTaskDefName());
		Assert.assertEquals(_id, _reportDTO.getId());
		Assert.assertEquals(_type, _reportDTO.getType());
		Assert.assertEquals(_arguments, _reportDTO.getArguments());
		Assert.assertEquals(_status, _reportDTO.getStatus());
		Assert.assertEquals(_date, _reportDTO.getDate());
		Assert.assertEquals(_duration, _reportDTO.getDuration());
		Assert.assertEquals(_rows, _reportDTO.getRows());
		Assert.assertEquals(_completed, _reportDTO.isCompleted());

	}
}
