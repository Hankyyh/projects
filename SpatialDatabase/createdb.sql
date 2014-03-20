CREATE TABLE building
(
  buildingNo VARCHAR(5) PRIMARY KEY,
  buildingName VARCHAR(100),
  buildingShape SDO_GEOMETRY NOT NULL);
  
CREATE TABLE hydrant
 (
   hydrantNo VARCHAR(5) PRIMARY KEY,
   hydrantLoc SDO_GEOMETRY NOT NULL); 
    
CREATE TABLE firebuilding
(
  buildingName VARCHAR(100));

COMMIT;

CREATE index buildingIndex on building(buildingShape) indextype is MDSYS.SPATIAL_INDEX;

CREATE index hydrantIndex on hydrant(hydrantLoc) indextype is MDSYS.SPATIAL_INDEX;

COMMIT;