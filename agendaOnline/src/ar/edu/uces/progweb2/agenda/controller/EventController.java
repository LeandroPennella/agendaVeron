package ar.edu.uces.progweb2.agenda.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;

import ar.edu.uces.progweb2.agenda.dto.FormDragEventDTO;
import ar.edu.uces.progweb2.agenda.dto.DragEventDTO;
import ar.edu.uces.progweb2.agenda.dto.FormCalendarDTO;
import ar.edu.uces.progweb2.agenda.dto.FormMeetingDTO;
import ar.edu.uces.progweb2.agenda.dto.FormPrivateEventDTO;
import ar.edu.uces.progweb2.agenda.exception.BackendException;
import ar.edu.uces.progweb2.agenda.model.Hall;
import ar.edu.uces.progweb2.agenda.model.User;
import ar.edu.uces.progweb2.agenda.service.EventService;
import ar.edu.uces.progweb2.agenda.service.HallService;
import ar.edu.uces.progweb2.agenda.utils.CalendarUtils;
import ar.edu.uces.progweb2.agenda.validator.MeetingValidator;
import ar.edu.uces.progweb2.agenda.validator.PrivateEventValidator;

@SessionAttributes("user")
@Controller
public class EventController {

	private HallService hallService;
	private EventService eventService;
	private PrivateEventValidator privateEventValidator;
	private MeetingValidator meetingValidator;

	@Autowired
	public void setHallService(HallService hallService) {
		this.hallService = hallService;
	}

	@Autowired
	public void setEventValidator(PrivateEventValidator privateEventValidator) {
		this.privateEventValidator = privateEventValidator;
	}

	@Autowired
	public void setMeetingValidator(MeetingValidator meetingValidator) {
		this.meetingValidator = meetingValidator;
	}

	@Autowired
	public void setEventService(EventService eventService) {
		this.eventService = eventService;
	}

	// asincronicos
	@RequestMapping(value = "/getEvents", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> getEvents(@RequestBody FormCalendarDTO form, ModelMap model) {
		User user = (User) model.get("user");
		Map<String, Object> map = new HashMap<String, Object>();
		Date date = CalendarUtils.getDate(form.getActualPage(),CalendarUtils.getDate(form.getDate()));
		Date week[] = CalendarUtils.getWeek(date);
		List<DragEventDTO> eventsSunday = this.eventService.getEvents(week[0], user);
		List<DragEventDTO> eventsMonday = this.eventService.getEvents(week[1], user);
		List<DragEventDTO> eventsTuesday = this.eventService.getEvents(week[2], user);
		List<DragEventDTO> eventsWednesday = this.eventService.getEvents(week[3], user);
		List<DragEventDTO> eventsThursday = this.eventService.getEvents(week[4], user);
		List<DragEventDTO> eventsFriday = this.eventService.getEvents(week[5], user);
		List<DragEventDTO> eventsSaturday = this.eventService.getEvents(week[6], user);
		
		map.put("eventsSunday", eventsSunday);
		map.put("eventsMonday", eventsMonday);
		map.put("eventsTuesday", eventsTuesday);
		map.put("eventsWednesday", eventsWednesday);
		map.put("eventsThursday", eventsThursday);
		map.put("eventsFriday", eventsFriday);
		map.put("eventsSaturday", eventsSaturday);
		map.put("week", CalendarUtils.convertDateToString(week));
		return map;
	}

	@RequestMapping(value = "/updateTimeEvent/{id}", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> updateTimeEvent(@PathVariable("id") Long id, @RequestBody FormDragEventDTO drag, ModelMap model) {
		Map<String, Object> map = new HashMap<String, Object>();
		User user = (User) model.get("user");
		drag.setUserLogin(user);
		try { 
			this.eventService.update(drag); 
			DragEventDTO event = this.eventService.getDraggableEvent(id);
			map.put("startTime", event.getStartTime());
			map.put("endTime", event.getEndTime());
		
		} catch (BackendException e) {
			e.printStackTrace();
			map.put("error", true);
			map.put("message", "Error al cambiar la hora de inicio de la reunion");
		}
		
		return map;
	}

	@RequestMapping(value = "/newMeeting")
	public String newMeeting(ModelMap model) {
		List<Hall> halls = this.hallService.getHalls();
		model.addAttribute("formMeeting", new FormMeetingDTO());
		model.addAttribute("halls", halls);
		model.addAttribute("hours", CalendarUtils.getHours());
		return "/jsp/meeting.jsp";
	}
	
	@RequestMapping(value = "/newPrivateEvent")
	public String newPrivateEvent(ModelMap model) {
		model.addAttribute("hours", CalendarUtils.getHours());
		model.addAttribute("formPrivateEvent", new FormPrivateEventDTO());
		return "/jsp/privateEvent.jsp";
	}

	@RequestMapping(value = "/saveMeeting")
	public String saveMeeting(@ModelAttribute("formMeeting") FormMeetingDTO eventDTO,
			ModelMap model, BindingResult result) {
		this.meetingValidator.validate(eventDTO, result);
		if (result.hasErrors()) {
			List<Hall> halls = this.hallService.getHalls();
			model.addAttribute("hours", CalendarUtils.getHours());
			model.addAttribute("halls", halls);
			return "/jsp/meeting.jsp";
		}
		User user = (User) model.get("user");
		eventDTO.setUserLogin(user);
		this.eventService.saveMeeting(eventDTO);
		return "redirect:/calendar.htm";
	}

	@RequestMapping(value = "/savePrivateEvent")
	public String savePrivateEvent(
			@ModelAttribute("formPrivateEvent") FormPrivateEventDTO eventDTO,
			ModelMap model, BindingResult result) {
		this.privateEventValidator.validate(eventDTO, result);
		if (result.hasErrors()) {
			List<Hall> halls = this.hallService.getHalls();
			model.addAttribute("halls", halls);
			model.addAttribute("hours", CalendarUtils.getHours());
			return "/jsp/privateEvent.jsp";
		}
		User user = (User) model.get("user");
		eventDTO.setUserLogin(user);
		this.eventService.savePrivateEvent(eventDTO);
		return "redirect:/calendar.htm";
	}
	
	@RequestMapping(value = "/detailMeeting")
	public String detailMeeting(@RequestParam("id") Long id, ModelMap model) {
		User user = (User) model.get("user");
		FormMeetingDTO form = this.eventService.getFormMeetingDTO(id, user );
		List<Hall> halls = this.hallService.getHalls();
		model.addAttribute("formMeeting", form);
		model.addAttribute("halls", halls);
		model.addAttribute("hours", CalendarUtils.getHours());
		if(form.getIsOwner()){
			model.addAttribute("url", "/updateMeeting.htm");
		} else {
			model.addAttribute("url", "/updateConfirm.htm");
		}
		return "/jsp/editMeeting.jsp";
	}
	
	@RequestMapping(value = "/detailPrivateEvent")
	public String detailPrivateEvent(@RequestParam("id") Long id, ModelMap model) {
		FormPrivateEventDTO form = this.eventService.getFormPrivateEventDTO(id);
		model.addAttribute("formPrivateEvent", form);
		model.addAttribute("hours", CalendarUtils.getHours());
		return "/jsp/editPrivateEvent.jsp";
	}
	
	@RequestMapping(value = "/updateMeeting")
	public String updateMeeting(@ModelAttribute("formMeeting") FormMeetingDTO eventDTO, ModelMap model, BindingResult result) {
		this.meetingValidator.validate(eventDTO, result);
		if (result.hasErrors()) {
			List<Hall> halls = this.hallService.getHalls();
			model.addAttribute("halls", halls);
			model.addAttribute("hours", CalendarUtils.getHours());
			return "/jsp/editMeeting.jsp";
		}
		User user = (User) model.get("user");
		eventDTO.setUserLogin(user);
		this.eventService.updateMeeting(eventDTO);
		return "redirect:/calendar.htm";
	}
	
	@RequestMapping(value = "/updatePrivateEvent")
	public String updatePrivateEvent(@ModelAttribute("formPrivateEvent") FormPrivateEventDTO eventDTO, ModelMap model, BindingResult result) {
		this.privateEventValidator.validate(eventDTO, result);
		if (result.hasErrors()) {
			List<Hall> halls = this.hallService.getHalls();
			model.addAttribute("halls", halls);
			model.addAttribute("hours", CalendarUtils.getHours());
			return "/jsp/editPrivateEvent.jsp";
		}
		User user = (User) model.get("user");
		eventDTO.setUserLogin(user);
		this.eventService.updatePrivateEvent(eventDTO);
		return "redirect:/calendar.htm";
	}
	
	@RequestMapping(value = "/deleteEvent")
	public String deleteEvent(@RequestParam("id") Long id) {
		this.eventService.delete(id);
		return "redirect:/calendar.htm";
	}
	
	@RequestMapping(value = "/updateConfirm")
	public String updateConfirm(@ModelAttribute("formMeeting") FormMeetingDTO eventDTO, ModelMap model) {
		User user = (User) model.get("user");
		eventDTO.setUserLogin(user);
		if(eventDTO.getState() == 1 || eventDTO.getState() == 2){
			this.eventService.setStateGuest(eventDTO);
		}
		return "redirect:/calendar.htm";
	}

}
