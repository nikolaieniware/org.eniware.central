ALTER TABLE eniwaredatum.da_datum
	DROP COLUMN jdata;

ALTER TABLE eniwareagg.agg_datum_hourly
	DROP COLUMN jdata;

ALTER TABLE eniwareagg.agg_datum_daily
	DROP COLUMN jdata;

ALTER TABLE eniwareagg.agg_datum_monthly
	DROP COLUMN jdata;


ALTER TABLE eniwaredatum.da_loc_datum
	DROP COLUMN jdata;

ALTER TABLE eniwareagg.agg_loc_datum_hourly
	DROP COLUMN jdata;

ALTER TABLE eniwareagg.agg_loc_datum_daily
	DROP COLUMN jdata;

ALTER TABLE eniwareagg.agg_loc_datum_monthly
	DROP COLUMN jdata;
