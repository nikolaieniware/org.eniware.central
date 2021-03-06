/* Example SQL to run after a manual data correction so aggregated tables are adjusted. */

select eniwarenet.populate_rep_power_datum_hourly(c) from eniwarenet.sn_power_datum c
where c.id in (
	select max(id) from eniwarenet.sn_power_datum
	where Edge_id = 19 and created between 
		('2010-01-28 17:00'::timestamp at time zone 'Australia/Sydney') and
		('2010-01-28 21:00'::timestamp at time zone 'Australia/Sydney')
	group by date_trunc('hour', created)
);

select eniwarenet.populate_rep_power_datum_daily(c) from eniwarenet.sn_power_datum c
where c.id in (
	select max(id) from eniwarenet.sn_power_datum
	where Edge_id = 19 and created between 
		('2010-01-28 17:00'::timestamp at time zone 'Australia/Sydney') and
		('2010-01-28 21:00'::timestamp at time zone 'Australia/Sydney')
	group by date_trunc('day', created)
);

select eniwarenet.populate_rep_net_power_datum_hourly(c) from eniwarenet.sn_power_datum c
where c.id in (
	select max(id) from eniwarenet.sn_power_datum
	where Edge_id = 19 and created between 
		('2010-01-28 17:00'::timestamp at time zone 'Australia/Sydney') and
		('2010-01-28 21:00'::timestamp at time zone 'Australia/Sydney')
	group by date_trunc('hour', created)
);

select eniwarenet.populate_rep_net_power_datum_daily(c) from eniwarenet.sn_power_datum c
where c.id in (
	select max(id) from eniwarenet.sn_power_datum
	where Edge_id = 19 and created between 
		('2010-01-28 17:00'::timestamp at time zone 'Australia/Sydney') and
		('2010-01-28 21:00'::timestamp at time zone 'Australia/Sydney')
	group by date_trunc('day', created)
);
