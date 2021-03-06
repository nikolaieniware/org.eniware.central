/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.query.web.api;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;

import org.eniware.central.datum.domain.EdgeSourcePK;
import org.eniware.central.query.domain.ReportableInterval;
import org.eniware.central.query.web.domain.GeneralReportableIntervalCommand;
import org.eniware.util.JodaDateFormatEditor;
import org.eniware.util.JodaDateFormatEditor.ParseMode;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.eniware.central.datum.biz.DatumMetadataBiz;
import org.eniware.central.query.biz.QueryBiz;
import org.eniware.central.web.support.WebServiceControllerSupport;
import org.eniware.web.domain.Response;

/**
 * Controller for querying for reportable interval values.
 * 
 * <p>
 * See the {@link ReportableInterval} class for information about what is
 * returned to the view.
 * </p>
 * 
 * @version 2.2
 */
@Controller("v1ReportableIntervalController")
@RequestMapping({ "/api/v1/sec/range", "/api/v1/pub/range" })
public class ReportableIntervalController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;
	private final DatumMetadataBiz datumMetadataBiz;
	private final PathMatcher pathMatcher;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 * @param datumMetadataBiz
	 *        the DatumMetadataBiz to use
	 */
	public ReportableIntervalController(QueryBiz queryBiz, DatumMetadataBiz datumMetadataBiz) {
		this(queryBiz, datumMetadataBiz, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 * @param datumMetadataBiz
	 *        the DatumMetadataBiz to use
	 * @param pathMatcher
	 *        the PathMatcher to use
	 * @since 2.2
	 */
	@Autowired
	public ReportableIntervalController(QueryBiz queryBiz, DatumMetadataBiz datumMetadataBiz,
			@Qualifier("sourceIdPathMatcher") PathMatcher pathMatcher) {
		super();
		this.queryBiz = queryBiz;
		this.datumMetadataBiz = datumMetadataBiz;
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Web binder initialization.
	 * 
	 * <p>
	 * Registers a {@link LocalDate} property editor using the
	 * {@link #DEFAULT_DATE_FORMAT} pattern.
	 * </p>
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(LocalDate.class,
				new JodaDateFormatEditor(DEFAULT_DATE_FORMAT, ParseMode.LocalDate));
		binder.registerCustomEditor(DateTime.class,
				new JodaDateFormatEditor(new String[] { DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_FORMAT },
						TimeZone.getTimeZone("UTC")));
	}

	/**
	 * Get a date range of available GeneralEdgeData for a Edge and an optional
	 * source ID.
	 * 
	 * <p>
	 * This method returns a start/end date range.
	 * </p>
	 * 
	 * <p>
	 * Example URL: <code>/api/v1/sec/range/interval?EdgeId=1</code>
	 * </p>
	 * 
	 * <p>
	 * Example JSON response:
	 * </p>
	 * 
	 * <pre>
	 * {
	 *   "success": true,
	 *   "data": {
	 *     "timeZone": "Pacific/Auckland",
	 *     "endDate": "2012-12-11 01:49",
	 *     "startDate": "2012-12-11 00:30",
	 *     "dayCount": 1683,
	 *     "monthCount": 56,
	 *     "yearCount": 6
	 *   }
	 * }
	 * </pre>
	 * 
	 * @param cmd
	 *        the input command
	 * @return the {@link ReportableInterval}
	 */
	@ResponseBody
	@RequestMapping(value = "/interval", method = RequestMethod.GET, params = "!types")
	public Response<ReportableInterval> getReportableInterval(GeneralReportableIntervalCommand cmd) {
		ReportableInterval data = queryBiz.getReportableInterval(cmd.getEdgeId(), cmd.getSourceId());
		return new Response<ReportableInterval>(data);
	}

	/**
	 * Get the set of source IDs available for the available GeneralEdgeData for
	 * a single Edge, optionally constrained within a date range.
	 * 
	 * <p>
	 * A <code>sourceId</code> path pattern may also be provided, to restrict
	 * the resulting source ID set to.
	 * </p>
	 * 
	 * <p>
	 * Example URL: <code>/api/v1/sec/range/sources?EdgeId=1</code>
	 * </p>
	 * 
	 * <p>
	 * Example JSON response:
	 * </p>
	 * 
	 * <pre>
	 * {
	 *   "success": true,
	 *   "data": [
	 *     "Main"
	 *   ]
	 * }
	 * </pre>
	 * 
	 * @param cmd
	 *        the input command
	 * @return the available sources
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET, params = { "!types",
			"!metadataFilter" })
	public Response<Set<String>> getAvailableSources(GeneralReportableIntervalCommand cmd) {
		Set<String> data = queryBiz.getAvailableSources(cmd.getEdgeId(), cmd.getStartDate(),
				cmd.getEndDate());

		// support filtering based on sourceId path pattern
		data = filterSources(data, this.pathMatcher, cmd.getSourceId());

		return new Response<Set<String>>(data);
	}

	/**
	 * Get all available Edge+source ID pairs that match a Edge datum metadata
	 * search filter.
	 * 
	 * <p>
	 * A <code>sourceId</code> path pattern may also be provided, to restrict
	 * the resulting source ID set to.
	 * </p>
	 * 
	 * <p>
	 * Example URL:
	 * <code>/api/v1/sec/range/sources?EdgeIds=1,2&metadataFilter=(/m/foo=bar)</code>
	 * </p>
	 *
	 * <p>
	 * Example JSON response:
	 * </p>
	 * 
	 * <pre>
	 * {
	 *   "success": true,
	 *   "data": [
	 *     { EdgeId: 1, sourceId: "A" },
	 *     { EdgeId: 2, "sourceId: "B" }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>
	 * If only a single Edge ID is specified, then the results will be
	 * simplified to just an array of strings for the source IDs, omitting the
	 * Edge ID which is redundant.
	 * </p>
	 * 
	 * @param cmd
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET, params = { "!types",
			"metadataFilter" })
	public Response<Set<?>> getMetadataFilteredAvailableSources(GeneralReportableIntervalCommand cmd) {
		Set<EdgeSourcePK> data = datumMetadataBiz
				.getGeneralEdgeDatumMetadataFilteredSources(cmd.getEdgeIds(), cmd.getMetadataFilter());

		// support filtering based on sourceId path pattern
		data = filterEdgeSources(data, this.pathMatcher, cmd.getSourceId());

		if ( cmd.getEdgeIds() != null && cmd.getEdgeIds().length < 2 ) {
			// at most 1 Edge ID, so simplify results to just source ID values
			Set<String> sourceIds = new LinkedHashSet<String>(data.size());
			for ( EdgeSourcePK pk : data ) {
				sourceIds.add(pk.getSourceId());
			}
			return new Response<Set<?>>(sourceIds);
		}
		return new Response<Set<?>>(data);
	}

	/**
	 * Filter a set of Edge sources using a source ID path pattern.
	 * 
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 * 
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 */
	public static Set<EdgeSourcePK> filterEdgeSources(Set<EdgeSourcePK> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<EdgeSourcePK> itr = sources.iterator(); itr.hasNext(); ) {
			EdgeSourcePK pk = itr.next();
			if ( !pathMatcher.match(pattern, pk.getSourceId()) ) {
				itr.remove();
			}
		}
		return sources;
	}

	/**
	 * Filter a set of sources using a source ID path pattern.
	 * 
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 * 
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 */
	public static Set<String> filterSources(Set<String> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<String> itr = sources.iterator(); itr.hasNext(); ) {
			String source = itr.next();
			if ( !pathMatcher.match(pattern, source) ) {
				itr.remove();
			}
		}
		return sources;
	}

}
