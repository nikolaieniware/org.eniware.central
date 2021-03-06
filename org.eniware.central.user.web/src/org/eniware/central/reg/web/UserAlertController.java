/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */
package org.eniware.central.reg.web;

import static org.eniware.web.domain.Response.response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.eniware.central.query.biz.QueryBiz;
import org.eniware.central.security.SecurityUser;
import org.eniware.central.security.SecurityUtils;
import org.eniware.web.domain.Response;

import org.eniware.central.user.biz.UserAlertBiz;
import org.eniware.central.user.biz.UserBiz;
import org.eniware.central.user.domain.UserAlert;
import org.eniware.central.user.domain.UserAlertOptions;
import org.eniware.central.user.domain.UserAlertSituationStatus;
import org.eniware.central.user.domain.UserAlertStatus;
import org.eniware.central.user.domain.UserAlertType;
import org.eniware.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for user alerts.
 * 
 * @version 1.1
 */
@Controller
@RequestMapping("/sec/alerts")
public class UserAlertController extends ControllerSupport {

	private static final Pattern TIME_PAT = Pattern.compile("[0-2]?\\d:[0-5]\\d");

	private final UserBiz userBiz;
	private final UserAlertBiz userAlertBiz;
	private final QueryBiz queryBiz;

	@Autowired
	public UserAlertController(UserBiz userBiz, UserAlertBiz userAlertBiz, QueryBiz queryBiz) {
		super();
		this.userBiz = userBiz;
		this.userAlertBiz = userAlertBiz;
		this.queryBiz = queryBiz;
	}

	@ModelAttribute("EdgeDataAlertTypes")
	public List<UserAlertType> EdgeDataAlertTypes() {
		// now now, only one alert type!
		return Collections.singletonList(UserAlertType.EdgeStaleData);
	}

	@ModelAttribute("alertStatuses")
	public UserAlertStatus[] alertStatuses() {
		return UserAlertStatus.values();
	}

	/**
	 * View the main Alerts screen.
	 * 
	 * @param model
	 *        The model object.
	 * @return The view name.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String view(Model model) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		List<UserAlert> alerts = userAlertBiz.userAlertsForUser(user.getUserId());
		if ( alerts != null ) {
			List<UserAlert> EdgeDataAlerts = new ArrayList<UserAlert>(alerts.size());
			for ( UserAlert alert : alerts ) {
				switch (alert.getType()) {
					case EdgeStaleData:
						EdgeDataAlerts.add(alert);
						break;
				}
			}
			model.addAttribute("EdgeDataAlerts", EdgeDataAlerts);
		}
		model.addAttribute("userEdges", userBiz.getUserEdges(user.getUserId()));
		return "alerts/view-alerts";
	}

	/**
	 * Get all available sources for a given Edge ID.
	 * 
	 * @param EdgeId
	 *        The ID of the Edge to get all available sources for.
	 * @param start
	 *        An optional start date to limit the query to.
	 * @param end
	 *        An optional end date to limit the query to.
	 * @return The found sources.
	 */
	@RequestMapping(value = "/Edge/{EdgeId}/sources", method = RequestMethod.GET)
	@ResponseBody
	public Response<List<String>> availableSourcesForEdge(@PathVariable("EdgeId") Long EdgeId,
			@RequestParam(value = "start", required = false) DateTime start,
			@RequestParam(value = "end", required = false) DateTime end) {
		Set<String> sources = queryBiz.getAvailableSources(EdgeId, start, end);
		List<String> sourceList = new ArrayList<String>(sources);
		return response(sourceList);
	}

	/**
	 * Get <em>active</em> situations for a given Edge.
	 * 
	 * @param EdgeId
	 *        The ID of the Edge to get the sitautions for.
	 * @param locale
	 *        The request locale.
	 * @return The alerts with active situations.
	 * @since 1.1
	 */
	@RequestMapping(value = "/Edge/{EdgeId}/situations", method = RequestMethod.GET)
	@ResponseBody
	public Response<List<UserAlert>> activeSituationsEdge(@PathVariable("EdgeId") Long EdgeId,
			Locale locale) {
		List<UserAlert> results = userAlertBiz.alertSituationsForEdge(EdgeId);
		for ( UserAlert alert : results ) {
			populateUsefulAlertOptions(alert, locale);
		}
		return response(results);
	}

	/**
	 * Get a count of <em>active</em> situations for the active user.
	 * 
	 * @return The count.
	 * @since 1.1
	 */
	@RequestMapping(value = "/user/situation/count", method = RequestMethod.GET)
	@ResponseBody
	public Response<Integer> activeSituationCount() {
		Long userId = SecurityUtils.getCurrentActorUserId();
		Integer count = userAlertBiz.alertSituationCountForUser(userId);
		return response(count);
	}

	/**
	 * Get <em>active</em> situations for the active user
	 * 
	 * @param locale
	 *        The request locale.
	 * @return The alerts with active situations.
	 * @since 1.1
	 */
	@RequestMapping(value = "/user/situations", method = RequestMethod.GET)
	@ResponseBody
	public Response<List<UserAlert>> activeSituations(Locale locale) {
		Long userId = SecurityUtils.getCurrentActorUserId();
		List<UserAlert> results = userAlertBiz.alertSituationsForUser(userId);
		for ( UserAlert alert : results ) {
			populateUsefulAlertOptions(alert, locale);
		}
		return response(results);
	}

	/**
	 * Create or update an alert.
	 * 
	 * @param model
	 *        The UserAlert details.
	 * @return The saved details.
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public Response<UserAlert> addAlert(@RequestBody UserAlert model) {
		final SecurityUser user = SecurityUtils.getCurrentUser();
		UserAlert alert = new UserAlert();
		alert.setId(model.getId());
		alert.setEdgeId(model.getEdgeId());
		alert.setUserId(user.getUserId());
		alert.setCreated(new DateTime());
		alert.setStatus(model.getStatus() == null ? UserAlertStatus.Active : model.getStatus());
		alert.setType(model.getType() == null ? UserAlertType.EdgeStaleData : model.getType());

		// reset validTo date to now, so alert re-processed
		alert.setValidTo(new DateTime());

		Map<String, Object> options = new HashMap<String, Object>();
		if ( model.getOptions() != null ) {
			for ( Map.Entry<String, Object> me : model.getOptions().entrySet() ) {
				if ( "ageMinutes".equalsIgnoreCase(me.getKey()) ) {
					// convert ageMinutes to age (seconds)
					Object v = model.getOptions().get("ageMinutes");
					double minutes = 1;
					try {
						minutes = Double.parseDouble(v.toString());
					} catch ( NumberFormatException e ) {
						// ignore
						log.warn("Alert option ageMinutes is not a number, setting to 1: [{}]", v);
					}
					options.put(UserAlertOptions.AGE_THRESHOLD, Math.round(minutes * 60.0));
				} else if ( "sources".equalsIgnoreCase(me.getKey()) && me.getValue() != null ) {
					// convert sources to List of String
					Set<String> sources = StringUtils
							.commaDelimitedStringToSet(me.getValue().toString());
					if ( sources != null ) {
						List<String> sourceList = new ArrayList<String>(sources);
						options.put(UserAlertOptions.SOURCE_IDS, sourceList);
					}
				} else if ( "windows".equalsIgnoreCase(me.getKey())
						&& me.getValue() instanceof Collection ) {
					@SuppressWarnings("unchecked")
					Collection<Map<String, ?>> windows = (Collection<Map<String, ?>>) me.getValue();
					List<Map<String, Object>> windowsList = new ArrayList<Map<String, Object>>();
					for ( Map<String, ?> window : windows ) {
						Object timeStart = window.get("timeStart");
						Object timeEnd = window.get("timeEnd");

						if ( timeStart != null && timeEnd != null ) {
							String ts = timeStart.toString();
							String te = timeEnd.toString();
							if ( TIME_PAT.matcher(ts).matches() && TIME_PAT.matcher(te).matches() ) {
								Map<String, Object> win = new LinkedHashMap<String, Object>(2);
								win.put("timeStart", ts);
								win.put("timeEnd", te);
								windowsList.add(win);
							}
						}
					}
					if ( windowsList.size() > 0 ) {
						options.put(UserAlertOptions.TIME_WINDOWS, windowsList);
					}
				}
			}
		}
		if ( options.size() > 0 ) {
			alert.setOptions(options);
		}

		Long id = userAlertBiz.saveAlert(alert);
		alert.setId(id);
		return response(alert);
	}

	private void populateUsefulAlertOptions(UserAlert alert, Locale locale) {
		if ( alert == null ) {
			return;
		}
		// to aid UI, populate some useful display properties
		if ( alert.getSituation() != null ) {
			DateTimeFormatter fmt = DateTimeFormat.forStyle("LS");
			if ( locale != null ) {
				fmt = fmt.withLocale(locale);
			}
			alert.getOptions().put("situationDate", fmt.print(alert.getSituation().getCreated()));
			if ( alert.getSituation().getNotified() != null ) {
				alert.getOptions().put("situationNotificationDate",
						fmt.print(alert.getSituation().getNotified()));
			}
		}
		if ( alert.getOptions() != null ) {
			Map<String, Object> options = alert.getOptions();
			if ( options.containsKey(UserAlertOptions.SOURCE_IDS) ) {
				@SuppressWarnings("unchecked")
				Collection<String> sources = (Collection<String>) options
						.get(UserAlertOptions.SOURCE_IDS);
				options.put("sources", StringUtils.commaDelimitedStringFromCollection(sources));
			}
		}
	}

	/**
	 * View an alert with the most recent active situation populated.
	 * 
	 * @param alertId
	 *        The ID of the alert to view.
	 * @param locale
	 *        The request locale.
	 * @return The alert.
	 */
	@RequestMapping(value = "/situation/{alertId}", method = RequestMethod.GET)
	@ResponseBody
	public Response<UserAlert> viewSituation(@PathVariable("alertId") Long alertId, Locale locale) {
		UserAlert alert = userAlertBiz.alertSituation(alertId);
		populateUsefulAlertOptions(alert, locale);
		return response(alert);
	}

	/**
	 * Update an active alert sitaution's status.
	 * 
	 * @param alertId
	 *        The ID of the alert with the active situation.
	 * @param status
	 *        The situation status to set.
	 * @param locale
	 *        The request locale.
	 * @return The updated alert.
	 */
	@RequestMapping(value = "/situation/{alertId}/resolve", method = RequestMethod.POST)
	@ResponseBody
	public Response<UserAlert> resolveSituation(@PathVariable("alertId") Long alertId,
			@RequestParam("status") UserAlertSituationStatus status, Locale locale) {
		UserAlert alert = userAlertBiz.updateSituationStatus(alertId, status);
		populateUsefulAlertOptions(alert, locale);
		return response(alert);
	}

	/**
	 * Delete an alert.
	 * 
	 * @param alertId
	 *        The ID of the alert to delete.
	 * @return The result.
	 */
	@RequestMapping(value = "/{alertId}", method = RequestMethod.DELETE)
	@ResponseBody
	public Response<Object> deleteAlert(@PathVariable("alertId") Long alertId) {
		userAlertBiz.deleteAlert(alertId);
		return response(null);
	}
}
