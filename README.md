# IIRS Survey App

### Introduction
An Android Application to help surveyors get a basic idea of the area they're surveying by getting basic information of the area, which includes soil characteristics, geomorphological and lithological characteristics, land usage, population in the area etc.<br/><br/> This project is a part of my Summer Internship Programme at the Indian Institute of Remote Sensing (IIRS), Dehradun. The app makes use of ArcGIS shapefiles to get the data of the Area. The App currently only works for Dehradun, Uttarakhand area due to limitation of the provided data. The ArcGIS Shapefiles were provided by IIRS.<br/><br/>
The App only works as medium to get the location of the user and show the resultant data. Everything including the working on the data in form of shapefiles and what data to show to the user is controlled by the REST API which has been implementated in Django with database in Spatialite which is an extension of SQlite.<br/>
### Data Used

##### Geomorphology Data
<img src="https://github.com/Abhishek-Kathayat/IIRSSurveyApp/blob/master/data/Geomorphology_Image.png" width="450" height="300">

##### Lithology Data
<img src="https://github.com/Abhishek-Kathayat/IIRSSurveyApp/blob/master/data/Lithology_Image.png" width="450" height="300">

##### Drainage Data
<img src="https://github.com/Abhishek-Kathayat/IIRSSurveyApp/blob/master/data/Drainage_Image.png" width="450" height="300">

##### Land Usage Data
<img src="https://github.com/Abhishek-Kathayat/IIRSSurveyApp/blob/master/data/LandUsage_Image.png" width="450" height="300">

### About the App
The app created for the project makes use of Mapbox API for getting the current location of the user. This location is sent to the Django API and the response recieved is displayed in a draggable bottom sheet using viewpager.<br/>
Screenshots of the app are shown below :<br/>
