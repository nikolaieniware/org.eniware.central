BEGIN;

/* === PRICE ============================================================= */

CREATE TABLE eniwarenet.sn_price_source (
	id				BIGINT NOT NULL DEFAULT nextval('eniwarenet.eniwarenet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	sname			CHARACTER VARYING(128) NOT NULL,
	PRIMARY KEY(id)
);

INSERT INTO eniwarenet.sn_price_source (sname) VALUES ('Unknown');
INSERT INTO eniwarenet.sn_price_source (sname) VALUES ('electricityinfo.co.nz');

CREATE TABLE eniwarenet.sn_price_loc (
	id				BIGINT NOT NULL DEFAULT nextval('eniwarenet.eniwarenet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_name		CHARACTER VARYING(128) NOT NULL UNIQUE,
	source_id		BIGINT NOT NULL,
	source_data		CHARACTER VARYING(128),
	currency		VARCHAR(10) NOT NULL,
	unit			VARCHAR(20) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT sn_price_loc_sn_price_source_fk FOREIGN KEY (source_id)
		REFERENCES eniwarenet.sn_price_source (id) MATCH SIMPLE
		ON UPDATE NO ACTION ON DELETE NO ACTION
);

INSERT INTO eniwarenet.sn_price_loc (loc_name, currency, unit, source_id) 
	VALUES ('Unknown', '--', '--', (SELECT id FROM eniwarenet.sn_price_source WHERE sname = 'Unknown'));
INSERT INTO eniwarenet.sn_price_loc (loc_name, currency, unit, source_id) 
	VALUES ('HAY2201', 'NZD', 'MWh', (SELECT id FROM eniwarenet.sn_price_source WHERE sname = 'electricityinfo.co.nz'));

/* ==========
 * MIGRATE EXISTING DATA
 * ========== */

CREATE TABLE eniwarenet.sn_price_datum_new (
	id				BIGINT NOT NULL DEFAULT nextval('eniwarenet.eniwarenet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	posted			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	loc_id 			BIGINT NOT NULL,
	price			REAL,
	error_msg		TEXT,
	prev_datum		BIGINT,
	PRIMARY KEY (id)
);

INSERT INTO eniwarenet.sn_price_datum_new
	SELECT d.id,d.created,d.posted,l.id,d.price,d.error_msg,d.prev_datum
	FROM eniwarenet.sn_price_datum d
	INNER JOIN eniwarenet.sn_price_loc l ON l.loc_name = 'HAY2201';

DROP FUNCTION IF EXISTS eniwarenet.find_prev_price_datum(eniwarenet.sn_price_datum, interval) CASCADE;
DROP FUNCTION IF EXISTS eniwarenet.populate_rep_price_datum_hourly(eniwarenet.sn_price_datum) CASCADE;
DROP FUNCTION IF EXISTS eniwarenet.populate_rep_price_datum_daily(eniwarenet.sn_price_datum) CASCADE;
DROP TABLE IF EXISTS eniwarenet.rep_price_datum_hourly CASCADE;
DROP TABLE IF EXISTS eniwarenet.rep_price_datum_daily CASCADE;

ALTER TABLE eniwarenet.sn_price_datum RENAME TO sn_price_datum_old;
ALTER TABLE eniwarenet.sn_price_datum_new RENAME TO sn_price_datum;
DROP TABLE eniwarenet.sn_price_datum_old CASCADE;

CREATE TABLE eniwarenet.sn_power_datum_new (
	id				BIGINT NOT NULL DEFAULT nextval('eniwarenet.eniwarenet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	posted			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	Edge_id 		BIGINT NOT NULL,
	source_id 		VARCHAR(255) NOT NULL DEFAULT '',
	price_loc_id	BIGINT,
	pv_volts 		REAL,
	pv_amps			REAL,
	bat_volts		REAL,
	bat_amp_hrs		REAL,
	dc_out_volts	REAL,
	dc_out_amps		REAL,
	ac_out_volts	REAL,
	ac_out_amps		REAL,
	amp_hours		REAL,
	kwatt_hours		REAL,
	error_msg		TEXT,
	prev_datum		BIGINT,
	local_created	TIMESTAMP WITHOUT TIME ZONE NOT NULL,
	PRIMARY KEY (id)
);

INSERT INTO eniwarenet.sn_power_datum_new
	SELECT p.id,p.created,p.posted,p.Edge_id,p.source_id,l.id,p.pv_volts,
	p.pv_amps,p.bat_volts,p.bat_amp_hrs,p.dc_out_volts,p.dc_out_amps,p.ac_out_volts,p.ac_out_amps,
	p.amp_hours,p.kwatt_hours,p.error_msg,p.prev_datum,p.local_created
	FROM eniwarenet.sn_power_datum p
	LEFT OUTER JOIN eniwarenet.sn_price_loc l ON l.loc_name = 'HAY2201' AND p.Edge_id = 11;

DROP FUNCTION IF EXISTS eniwarenet.find_prev_power_datum(eniwarenet.sn_power_datum, interval) CASCADE;
DROP FUNCTION IF EXISTS eniwarenet.find_rep_price_datum(bigint, timestamp without time zone, text, interval) CASCADE;
DROP FUNCTION IF EXISTS eniwarenet.populate_rep_power_datum_hourly(eniwarenet.sn_power_datum) CASCADE;
DROP FUNCTION IF EXISTS eniwarenet.populate_rep_power_datum_daily(eniwarenet.sn_power_datum) CASCADE;

ALTER TABLE eniwarenet.sn_power_datum RENAME TO sn_power_datum_old;
ALTER TABLE eniwarenet.sn_power_datum_new RENAME TO sn_power_datum;
DROP TABLE eniwarenet.sn_power_datum_old CASCADE;


CREATE TABLE eniwarenet.sn_consum_datum_new (
	id				BIGINT NOT NULL DEFAULT nextval('eniwarenet.eniwarenet_seq'),
	created			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	posted			TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
	Edge_id 		BIGINT NOT NULL,
	source_id 		VARCHAR(255) NOT NULL,
	price_loc_id	BIGINT,
	amps			REAL,
	voltage			REAL,
	error_msg		TEXT,
	prev_datum		BIGINT,
	PRIMARY KEY (id)
);

INSERT INTO eniwarenet.sn_consum_datum_new
	SELECT c.id,c.created,c.posted,c.Edge_id,c.source_id,l.id,c.amps,
	c.voltage,c.error_msg,c.prev_datum
	FROM eniwarenet.sn_consum_datum c
	LEFT OUTER JOIN eniwarenet.sn_price_loc l ON l.loc_name = 'HAY2201' AND c.Edge_id = 11;

DROP FUNCTION IF EXISTS eniwarenet.find_prev_consum_datum(eniwarenet.sn_consum_datum, interval) CASCADE;
DROP FUNCTION IF EXISTS eniwarenet.populate_rep_consum_datum_hourly(eniwarenet.sn_consum_datum) CASCADE;
DROP FUNCTION IF EXISTS eniwarenet.populate_rep_consum_datum_daily(eniwarenet.sn_consum_datum) CASCADE;

ALTER TABLE eniwarenet.sn_consum_datum RENAME TO sn_consum_datum_old;
ALTER TABLE eniwarenet.sn_consum_datum_new RENAME TO sn_consum_datum;
DROP TABLE eniwarenet.sn_consum_datum_old CASCADE;

/* ==========
 * Add constraints back to new tables
 * ========== */

ALTER TABLE eniwarenet.sn_price_datum ADD CONSTRAINT sn_price_datum_price_loc_fk
FOREIGN KEY (loc_id) REFERENCES eniwarenet.sn_price_loc (id);

CREATE UNIQUE INDEX price_datum_unq_idx ON eniwarenet.sn_price_datum (created, loc_id);
CREATE INDEX price_datum_prev_datum_idx ON eniwarenet.sn_price_datum (prev_datum);

CLUSTER eniwarenet.sn_price_datum USING price_datum_unq_idx;

ALTER TABLE eniwarenet.sn_power_datum ADD CONSTRAINT sn_power_datum_Edge_fk
FOREIGN KEY (Edge_id) REFERENCES eniwarenet.sn_Edge (Edge_id);
ALTER TABLE eniwarenet.sn_power_datum ADD CONSTRAINT sn_power_datum_price_loc_fk
FOREIGN KEY (price_loc_id) REFERENCES eniwarenet.sn_price_loc (id);

CREATE UNIQUE INDEX power_datum_Edge_unq_idx ON eniwarenet.sn_power_datum (created, Edge_id, source_id);
CREATE INDEX power_datum_prev_datum_idx ON eniwarenet.sn_power_datum (prev_datum);
CREATE INDEX power_datum_local_created_idx ON eniwarenet.sn_power_datum (local_created);

CLUSTER eniwarenet.sn_power_datum USING power_datum_Edge_unq_idx;

ALTER TABLE eniwarenet.sn_consum_datum ADD CONSTRAINT sn_consum_datum_Edge_fk
FOREIGN KEY (Edge_id) REFERENCES eniwarenet.sn_Edge (Edge_id);
ALTER TABLE eniwarenet.sn_consum_datum ADD CONSTRAINT sn_consum_datum_price_loc_fk
FOREIGN KEY (price_loc_id) REFERENCES eniwarenet.sn_price_loc (id);

CREATE UNIQUE INDEX consum_datum_Edge_unq_idx ON eniwarenet.sn_consum_datum (created,Edge_id,source_id);
CREATE INDEX consum_datum_prev_datum_idx ON eniwarenet.sn_consum_datum (prev_datum);

CLUSTER eniwarenet.sn_consum_datum USING consum_datum_Edge_unq_idx;


/* ==========
 * Recreate price reporting tables, functions
 * ========== */

CREATE OR REPLACE FUNCTION eniwarenet.find_prev_price_datum(eniwarenet.sn_price_datum, interval)
  RETURNS bigint AS
$BODY$
	SELECT c.id
	FROM eniwarenet.sn_price_datum c
	WHERE c.created < $1.created
		AND c.created >= ($1.created - $2)
		AND c.loc_id = $1.loc_id
	ORDER BY c.created DESC
	LIMIT 1;
$BODY$
  LANGUAGE 'sql' STABLE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_prev_price_datum()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.prev_datum := eniwarenet.find_prev_price_datum(NEW, interval '1 hour');
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_prev_price_datum
  BEFORE INSERT
  ON eniwarenet.sn_price_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_prev_price_datum();




CREATE TABLE eniwarenet.rep_price_datum_hourly (
  created_hour 	TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  loc_id		BIGINT NOT NULL,
  price			REAL,
  CONSTRAINT rep_price_datum_hourly_pkey PRIMARY KEY (created_hour, loc_id),
  CONSTRAINT rep_price_datum_hourly_price_loc_fk FOREIGN KEY (loc_id)
      REFERENCES eniwarenet.sn_price_loc (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

COMMENT ON TABLE eniwarenet.rep_price_datum_hourly IS 'To refresh this table, call something like this:

select eniwarenet.populate_rep_price_datum_hourly(c) from eniwarenet.sn_price_datum c
where c.id in (
	select max(id) from eniwarenet.sn_price_datum
	group by date_trunc(''hour'', created), Edge_id
)';

CLUSTER eniwarenet.rep_price_datum_hourly USING rep_price_datum_hourly_pkey;

CREATE TABLE eniwarenet.rep_price_datum_daily (
  created_day 	DATE NOT NULL,
  loc_id		BIGINT NOT NULL,
  price			REAL,
  CONSTRAINT rep_price_datum_daily_pkey PRIMARY KEY (created_day, loc_id),
  CONSTRAINT rep_price_datum_daily_price_loc_fk FOREIGN KEY (loc_id)
      REFERENCES eniwarenet.sn_price_loc (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

COMMENT ON TABLE eniwarenet.rep_price_datum_daily IS 'To refresh this table, call something like this:

select eniwarenet.populate_rep_price_datum_daily(c) from eniwarenet.sn_price_datum c
where c.id in (
	select max(id) from eniwarenet.sn_price_datum
	group by date_trunc(''hour'', created), Edge_id
)';

CLUSTER eniwarenet.rep_price_datum_daily USING rep_price_datum_daily_pkey;

/**************************************************************************************************
 * FUNCTION eniwarenet.find_rep_price_datum(bigint timestamp, interval)
 * 
 * SQL function to return a set of reporting PriceDatum records for a specific Edge within a given
 * date range.
 * 
 * @param bigint		the location ID
 * @param timestamp		the starting date
 * @param interval		an interval to calculate the end date from the starting date
 */
CREATE OR REPLACE FUNCTION eniwarenet.find_rep_price_datum(IN bigint, IN timestamp with time zone, IN interval)
  RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_price 		double precision
) AS
$BODY$
	SELECT 
		c2.created as created,
		(c.price + c2.price) / 2 as avg_price
	FROM eniwarenet.sn_price_datum c
	INNER JOIN eniwarenet.sn_price_datum c2 ON c2.id = c.prev_datum
	WHERE 
		c2.loc_id = $1
		AND c2.created >= $2
		AND c2.created < $2 + $3
	ORDER BY c.created
$BODY$
LANGUAGE 'sql' STABLE;

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_price_datum_hourly(eniwarenet.sn_price_datum)
 * 
 * Pl/PgSQL function to aggreage PriceDatum rows at an hourly level and populate the results into
 * the eniwarenet.rep_price_datum_hourly table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_price_datum table and thus treat the rep_price_datum_hourly
 * table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_price_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_power_datum		the PriceDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_price_datum_hourly(datum eniwarenet.sn_price_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp with time zone;
	data eniwarenet.rep_price_datum_hourly;
BEGIN
	SELECT date_trunc('hour', c.created)
	FROM eniwarenet.sn_price_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created) as created_hour,
		datum.loc_id,
		avg(sub.avg_price) as price
	FROM eniwarenet.find_rep_price_datum(datum.loc_id, chour, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created)
	ORDER BY date_trunc('hour', sub.created)
	INTO data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_price_datum_hourly SET
			price = data.price
		WHERE created_hour = data.created_hour
			AND loc_id = data.loc_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_price_datum_hourly (
			created_hour, loc_id, price)
		VALUES (data.created_hour, data.loc_id, data.price);

		EXIT insert_update;
	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_price_hourly()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_price_datum_hourly(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_price_hourly
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_price_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_price_hourly();

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_price_datum_daily(eniwarenet.sn_price_datum)
 * 
 * Pl/PgSQL function to aggreage PriceDatum rows at an daily level and populate the results into
 * the eniwarenet.rep_price_datum_daily table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_price_datum table and thus treat the rep_price_datum_daily
 * table like a materialized view.
 * 
 * The day to update is derived from the created column of the input record's "previous" record
 * (the sn_price_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_power_datum		the PriceDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_price_datum_daily(datum eniwarenet.sn_price_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp with time zone;
	data eniwarenet.rep_price_datum_daily;
BEGIN
	SELECT date_trunc('day', c.created)
	FROM eniwarenet.sn_price_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created) as created_day,
		datum.loc_id,
		avg(sub.avg_price) as price
	FROM eniwarenet.find_rep_price_datum(datum.loc_id, chour, interval '1 day') AS sub
	GROUP BY date(sub.created)
	ORDER BY date(sub.created)
	INTO data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_price_datum_daily SET
			price = data.price
		WHERE created_day = data.created_day
			AND loc_id = data.loc_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_price_datum_daily (
			created_day, loc_id, price)
		VALUES (data.created_day, data.loc_id, data.price);

		EXIT insert_update;
	END LOOP insert_update;
END;$BODY$
LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_price_daily()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_price_datum_daily(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_price_daily
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_price_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_price_daily();

/* ========
 * Update consumption
 * ======== */
 
CREATE OR REPLACE FUNCTION eniwarenet.find_prev_consum_datum(eniwarenet.sn_consum_datum, interval)
  RETURNS bigint AS
$BODY$
	SELECT c.id
	FROM eniwarenet.sn_consum_datum c
	WHERE c.created < $1.created
		AND c.created >= ($1.created - $2)
		AND c.Edge_id = $1.Edge_id
		AND c.source_id = $1.source_id
	ORDER BY c.created DESC
	LIMIT 1;
$BODY$
  LANGUAGE 'sql' STABLE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_prev_consum_datum()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.prev_datum := eniwarenet.find_prev_consum_datum(NEW, interval '1 hour');
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_prev_consum_datum
  BEFORE INSERT
  ON eniwarenet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_prev_consum_datum();

 
/**************************************************************************************************
 * FUNCTION eniwarenet.find_rep_consum_datum(bigint, text, timestamp, text, interval)
 * 
 * SQL function to return a set of reporting ConsumptionDatum records for a specific Edge within a
 * given date range.
 * 
 * @param bigint		the Edge ID
 * @param text			the source ID
 * @param timestamp		the starting date
 * @param text			a valid time zone ID (e.g. 'Pacific/Auckland')
 * @param interval		an interval to calculate the end date from the starting date
 */
CREATE OR REPLACE FUNCTION eniwarenet.find_rep_consum_datum(IN bigint, IN text, 
	IN timestamp without time zone, IN text, IN interval)
RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_amps 		double precision, 
	avg_voltage 	double precision, 
	watt_hours		double precision,
	price_per_Wh	double precision,
	cost_amt		double precision,
	currency		character varying(10)
) AS
$BODY$
	SELECT DISTINCT ON (c.created)
		c2.created as created,
		(c.amps + c2.amps) / 2 as avg_amps,
		(c.voltage + c2.voltage) / 2 as avg_voltage,
		eniwarenet.calc_avg_watt_hours(c.amps, c2.amps, c.voltage, c2.voltage, 
			NULL, NULL, (c.created - c2.created)) as watt_hours,
		eniwarenet.calc_price_per_watt_hours(p.price, p.unit) as price_per_Wh,
		eniwarenet.calc_price_per_watt_hours(p.price, p.unit) * 
			eniwarenet.calc_avg_watt_hours(c.amps, c2.amps, c.voltage, c2.voltage, 
			NULL, NULL, (c.created - c2.created)) as cost_amt,
		p.currency
	FROM eniwarenet.sn_consum_datum c
	LEFT OUTER JOIN eniwarenet.sn_consum_datum c2 ON c2.id = c.prev_datum
	LEFT OUTER JOIN (
		SELECT p.created, p.price, l.id, l.unit, l.currency
		FROM eniwarenet.sn_price_datum p
		INNER JOIN eniwarenet.sn_price_loc l ON l.id = p.loc_id
		WHERE p.created between (($3  - interval '1 hour') at time zone $4) 
				and (($3 + $5) at time zone $4)
		) AS p ON p.created BETWEEN c.created - interval '1 hour' AND c.created
			AND p.id = c.price_loc_id
	WHERE 
		c2.Edge_id = $1
		AND c2.source_id = $2
		AND c2.created >= $3 at time zone $4
		AND c2.created < $3 at time zone $4 + $5
	ORDER BY c.created, p.created DESC
$BODY$
LANGUAGE 'sql' STABLE;

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_consum_datum_hourly(eniwarenet.sn_consum_datum)
 * 
 * Pl/PgSQL function to aggreage ConsumptionDatum rows at an hourly level and populate the results
 * into the eniwarenet.rep_consum_datum_hourly table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_consum_datum table and thus treat the rep_consum_datum_hourly
 * table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_consum_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_consum_datum		the ConsumptionDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_consum_datum_hourly(datum eniwarenet.sn_consum_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	Edge_tz text;
	data eniwarenet.rep_consum_datum_hourly;
BEGIN
	SELECT time_zone from eniwarenet.sn_Edge where Edge_id = datum.Edge_id
	INTO Edge_tz;
	
	SELECT date_trunc('hour', c.created at time zone Edge_tz)
	FROM eniwarenet.sn_consum_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created at time zone Edge_tz) as created_hour,
		datum.Edge_id,
		datum.source_id,
		avg(sub.avg_amps) as amps,
		avg(sub.avg_voltage) as voltage,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM eniwarenet.find_rep_consum_datum(datum.Edge_id, datum.source_id, chour, Edge_tz, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created at time zone Edge_tz)
	ORDER BY date_trunc('hour', sub.created at time zone Edge_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_consum_datum_hourly SET
			amps = data.amps, 
			voltage = data.voltage,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_hour = data.created_hour
			AND Edge_id = data.Edge_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_consum_datum_hourly (
			created_hour, Edge_id, source_id, amps, voltage, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_hour, data.Edge_id, data.source_id, 	
			data.amps, data.voltage, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_consum_hourly()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_consum_datum_hourly(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_consum_hourly
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_consum_hourly();

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_consum_datum_daily(eniwarenet.sn_consum_datum)
 * 
 * Pl/PgSQL function to aggreage ConsumptionDatum rows at an daily level and populate the results
 * into the eniwarenet.rep_consum_datum_daily table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_consum_datum table and thus treat the rep_consum_datum_daily
 * table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_consum_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_consum_datum		the ConsumptionDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_consum_datum_daily(datum eniwarenet.sn_consum_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	Edge_tz text;
	data eniwarenet.rep_consum_datum_daily;
BEGIN
	SELECT time_zone from eniwarenet.sn_Edge where Edge_id = datum.Edge_id
	INTO Edge_tz;
	
	SELECT date_trunc('day', c.created at time zone Edge_tz)
	FROM eniwarenet.sn_consum_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created at time zone Edge_tz) as created_day,
		datum.Edge_id,
		datum.source_id,
		avg(sub.avg_amps) as amps,
		avg(sub.avg_voltage) as voltage,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM eniwarenet.find_rep_consum_datum(datum.Edge_id, datum.source_id, chour, Edge_tz, interval '1 day') AS sub
	GROUP BY date(sub.created at time zone Edge_tz)
	ORDER BY date(sub.created at time zone Edge_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_consum_datum_daily SET
			amps = data.amps, 
			voltage = data.voltage,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_day = data.created_day
			AND Edge_id = data.Edge_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_consum_datum_daily (
			created_day, Edge_id, source_id, amps, voltage, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_day, data.Edge_id, data.source_id, 	
			data.amps, data.voltage, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;
	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_consum_daily()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_consum_datum_daily(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_consum_daily
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_consum_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_consum_daily();

/* ========
 * Update power
 * ======== */
 
CREATE OR REPLACE FUNCTION eniwarenet.find_prev_power_datum(eniwarenet.sn_power_datum, interval)
  RETURNS bigint AS
$BODY$
	SELECT c.id
	FROM eniwarenet.sn_power_datum c
	WHERE c.created < $1.created
		AND c.created >= ($1.created - $2)
		AND c.Edge_id = $1.Edge_id
		AND c.source_id = $1.source_id
	ORDER BY c.created DESC
	LIMIT 1;
$BODY$
  LANGUAGE 'sql' STABLE;

COMMENT ON FUNCTION eniwarenet.find_prev_power_datum(eniwarenet.sn_power_datum, interval) IS 
'To manually update power datum, use a query like:

update eniwarenet.sn_power_datum set prev_datum = eniwarenet.find_prev_power_datum(sn_power_datum, 
	interval ''1 hour'');';

CREATE OR REPLACE FUNCTION eniwarenet.populate_prev_power_datum()
  RETURNS "trigger" AS
$BODY$
BEGIN
	NEW.prev_datum := eniwarenet.find_prev_power_datum(NEW, interval '1 hour');
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_prev_power_datum
  BEFORE INSERT
  ON eniwarenet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_prev_power_datum();

CREATE OR REPLACE FUNCTION eniwarenet.populate_local_created()
  RETURNS trigger AS
$BODY$
BEGIN
	NEW.local_created := eniwarenet.get_Edge_local_timestamp(NEW.created, NEW.Edge_id);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_local_created
   BEFORE INSERT OR UPDATE
   ON eniwarenet.sn_power_datum FOR EACH ROW
   EXECUTE PROCEDURE eniwarenet.populate_local_created();



/**************************************************************************************************
 * FUNCTION eniwarenet.find_rep_power_datum(bigint, text, timestamp, text, interval)
 * 
 * SQL function to return a set of reporting PowerDatum records for a specific Edge within a given
 * date range.
 * 
 * @param bigint		the Edge ID
 * @param text			the source ID
 * @param timestamp		the starting date
 * @param text			a valid time zone ID (e.g. 'Pacific/Auckland')
 * @param interval		an interval to calculate the end date from the starting date
 */
CREATE OR REPLACE FUNCTION eniwarenet.find_rep_power_datum(IN bigint, IN text, IN timestamp without time zone, IN text, IN interval)
  RETURNS TABLE(
	created 		timestamp with time zone, 
	avg_pv_amps 	double precision, 
	avg_pv_volts 	double precision, 
	avg_bat_volts 	double precision,
	watt_hours		double precision,
	price_per_Wh	double precision,
	cost_amt		double precision,
	currency		character varying(10)
) AS
$BODY$
	SELECT DISTINCT ON (c.created)
		c2.created as created,
		(c.pv_amps + c2.pv_amps) / 2 as avg_pv_amps,
		(c.pv_volts + c2.pv_volts) / 2 as avg_pv_volts,
		(c.bat_volts + c2.bat_volts) / 2 as avg_bat_volts,
		eniwarenet.calc_avg_watt_hours(c.pv_amps, c2.pv_amps, c.pv_volts, c2.pv_volts, 
			c.kwatt_hours, c2.kwatt_hours, (c.created - c2.created)) as watt_hours,
		eniwarenet.calc_price_per_watt_hours(p.price, p.unit) as price_per_Wh,
		eniwarenet.calc_price_per_watt_hours(p.price, p.unit) * 
			eniwarenet.calc_avg_watt_hours(c.pv_amps, c2.pv_amps, c.pv_volts, c2.pv_volts, 
			c.kwatt_hours, c2.kwatt_hours, (c.created - c2.created)) as cost_amt,
		p.currency
	FROM eniwarenet.sn_power_datum c
	LEFT OUTER JOIN eniwarenet.sn_power_datum c2 ON c2.id = c.prev_datum
	LEFT OUTER JOIN (
		SELECT p.created, l.id, p.price, l.unit, l.currency
		FROM eniwarenet.sn_price_datum p
		INNER JOIN eniwarenet.sn_price_loc l ON l.id = p.loc_id
		WHERE p.created between (($3  - interval '1 hour') at time zone $4) 
			and (($3 + $5) at time zone $4)
		) AS p ON p.created BETWEEN c.created - interval '1 hour' AND c.created
			AND p.id = c.price_loc_id
	WHERE 
		c2.Edge_id = $1
		AND c2.source_id = $2
		AND c2.created >= $3 at time zone $4
		AND c2.created < $3 at time zone $4 + $5
	ORDER BY c.created, p.created DESC
$BODY$
LANGUAGE 'sql' STABLE;

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_power_datum_hourly(eniwarenet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage PowerDatum rows at an hourly level and populate the results into
 * the eniwarenet.rep_power_datum_hourly table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_power_datum table and thus treat the rep_power_datum_hourly
 * table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_power_datum		the PowerDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_power_datum_hourly(datum eniwarenet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	Edge_tz text;
	data eniwarenet.rep_power_datum_hourly;
BEGIN
	SELECT time_zone from eniwarenet.sn_Edge where Edge_id = datum.Edge_id
	INTO Edge_tz;
	
	SELECT date_trunc('hour', c.created at time zone Edge_tz)
	FROM eniwarenet.sn_power_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created at time zone Edge_tz) as created_hour,
		datum.Edge_id,
		datum.source_id,
		avg(sub.avg_pv_volts) as pv_volts,
		avg(sub.avg_pv_amps) as pv_amps,
		avg(sub.avg_bat_volts) as bat_volts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM eniwarenet.find_rep_power_datum(datum.Edge_id, datum.source_id, chour, Edge_tz, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created at time zone Edge_tz)
	ORDER BY date_trunc('hour', sub.created at time zone Edge_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_power_datum_hourly SET
			pv_amps = data.pv_amps, 
			pv_volts = data.pv_volts,
			bat_volts = data.bat_volts,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_hour = data.created_hour
			AND Edge_id = data.Edge_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_power_datum_hourly (
			created_hour, Edge_id, source_id, pv_amps, pv_volts, bat_volts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_hour, data.Edge_id, data.source_id,	
			data.pv_amps, data.pv_volts, data.bat_volts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_power_hourly()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_power_datum_hourly(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_power_hourly
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_power_hourly();

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_net_power_datum_hourly(eniwarenet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage PowerDatum rows at an hourly level and populate the results into
 * the eniwarenet.rep_net_power_datum_hourly table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_power_datum table and thus treat the 
 * rep_net_power_datum_hourly table like a materialized view.
 * 
 * The hour to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_power_datum		the PowerDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_net_power_datum_hourly(datum eniwarenet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	data eniwarenet.rep_net_power_datum_hourly;
BEGIN
	SELECT date_trunc('hour', c.created at time zone n.time_zone)
	FROM eniwarenet.sn_power_datum c
	INNER JOIN eniwarenet.sn_Edge n ON n.Edge_id = datum.Edge_id
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date_trunc('hour', sub.created) as created_hour,
		datum.Edge_id,
		sum(sub.watt_hours) as watt_hours
	FROM eniwarenet.find_rep_net_power_datum(chour, interval '1 hour') AS sub
	GROUP BY date_trunc('hour', sub.created)
	ORDER BY date_trunc('hour', sub.created)
	INTO data;

	IF NOT FOUND THEN
		RAISE NOTICE 'Datum % has insufficient data.', datum;
		RETURN;
	END IF;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_net_power_datum_hourly SET
			watt_hours = data.watt_hours
		WHERE created_hour = data.created_hour;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_net_power_datum_hourly (
			created_hour, watt_hours)
		VALUES (data.created_hour, data.watt_hours);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_net_power_hourly()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_net_power_datum_hourly(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_net_power_hourly
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_net_power_hourly();

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_power_datum_daily(eniwarenet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage PowerDatum rows at an daily level and populate the results into
 * the eniwarenet.rep_power_datum_daily table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_power_datum table and thus treat the rep_power_datum_daily
 * table like a materialized view.
 * 
 * The day to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_power_datum		the PowerDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_power_datum_daily(datum eniwarenet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	Edge_tz text;
	data eniwarenet.rep_power_datum_daily;
BEGIN
	SELECT time_zone from eniwarenet.sn_Edge where Edge_id = datum.Edge_id
	INTO Edge_tz;
	
	SELECT date_trunc('day', c.created at time zone Edge_tz)
	FROM eniwarenet.sn_power_datum c
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created at time zone Edge_tz) as created_day,
		datum.Edge_id,
		datum.source_id,
		avg(sub.avg_pv_volts) as pv_volts,
		avg(sub.avg_pv_amps) as pv_amps,
		avg(sub.avg_bat_volts) as bat_volts,
		sum(sub.watt_hours) as watt_hours,
		sum(sub.cost_amt) as cost_amt,
		min(sub.currency) as cost_currency
	FROM eniwarenet.find_rep_power_datum(datum.Edge_id, datum.source_id, chour, Edge_tz, interval '1 day') AS sub
	GROUP BY date(sub.created at time zone Edge_tz)
	ORDER BY date(sub.created at time zone Edge_tz)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_power_datum_daily SET
			pv_amps = data.pv_amps, 
			pv_volts = data.pv_volts,
			bat_volts = data.bat_volts,
			watt_hours = data.watt_hours,
			cost_amt = data.cost_amt,
			cost_currency = data.cost_currency
		WHERE created_day = data.created_day
			AND Edge_id = data.Edge_id
			AND source_id = data.source_id;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_power_datum_daily (
			created_day, Edge_id, source_id, pv_amps, pv_volts, bat_volts, 
			watt_hours, cost_amt, cost_currency)
		VALUES (data.created_day, data.Edge_id, data.source_id,
			data.pv_amps, data.pv_volts, data.bat_volts, 
			data.watt_hours, data.cost_amt, data.cost_currency);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_power_daily()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_power_datum_daily(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_power_daily
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_power_daily();

/**************************************************************************************************
 * FUNCTION eniwarenet.populate_rep_net_power_datum_daily(eniwarenet.sn_power_datum)
 * 
 * Pl/PgSQL function to aggreage PowerDatum rows at an daily level and populate the results into
 * the eniwarenet.rep_net_power_datum_daily table. This function is designed to be exectued via a 
 * trigger function on the eniwarenet.sn_power_datum table and thus treat the 
 * rep_net_power_datum_daily table like a materialized view.
 * 
 * The day to update is derived from the created column of the input record's "previous" record
 * (the sn_power_datum.prev_datum ID).
 * 
 * @param eniwarenet.sn_power_datum		the PowerDatum row to update aggregated data for
 */
CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_net_power_datum_daily(datum eniwarenet.sn_power_datum)
  RETURNS void AS
$BODY$
DECLARE
	chour timestamp;
	data eniwarenet.rep_net_power_datum_daily;
BEGIN
	SELECT date_trunc('day', c.created at time zone n.time_zone)
	FROM eniwarenet.sn_power_datum c
	INNER JOIN eniwarenet.sn_Edge n ON n.Edge_id = datum.Edge_id
	WHERE c.id = datum.prev_datum
	INTO chour;

	IF NOT FOUND THEN
		--RAISE NOTICE 'Datum % has no previous datum.', datum;
		RETURN;
	END IF;

	SELECT 
		date(sub.created) as created_day,
		sum(sub.watt_hours) as watt_hours
	FROM eniwarenet.find_rep_net_power_datum(chour, interval '1 day') AS sub
	GROUP BY date(sub.created)
	ORDER BY date(sub.created)
	INTO data;
	--RAISE NOTICE 'Got data: %', data;
	
	<<insert_update>>
	LOOP
		UPDATE eniwarenet.rep_net_power_datum_daily SET
			watt_hours = data.watt_hours
		WHERE created_day = data.created_day;
		
		EXIT insert_update WHEN FOUND;

		INSERT INTO eniwarenet.rep_net_power_datum_daily (
			created_day, watt_hours)
		VALUES (data.created_day, data.watt_hours);

		EXIT insert_update;

	END LOOP insert_update;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE OR REPLACE FUNCTION eniwarenet.populate_rep_net_power_daily()
  RETURNS "trigger" AS
$BODY$BEGIN
	PERFORM eniwarenet.populate_rep_net_power_datum_daily(NEW);
	RETURN NEW;
END;$BODY$
  LANGUAGE 'plpgsql' VOLATILE;

CREATE TRIGGER populate_rep_net_power_daily
  AFTER INSERT OR UPDATE
  ON eniwarenet.sn_power_datum
  FOR EACH ROW
  EXECUTE PROCEDURE eniwarenet.populate_rep_net_power_daily();

