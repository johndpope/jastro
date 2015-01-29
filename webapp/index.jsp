<html>
  <head>
    <title>AstrologyClock Chart Generator</title>
    <style type="text/css">
      body
      {
        font-family: trebuchet ms,arial,helvetica;
        font-size: 15px;
      }
      div.examples img
      {
        vertical-align: middle;
      }
      div.sub
      {
        border-left: solid 8px #e0e0ff;
        padding-left: 100px;
      }
      em
      {
        font-family: courier, fixed;
        font-size: 10px;
        color: #80a080;
      }
   </style>
   <script type="text/javascript" src="jquery.js">//</script>
 </head>
 <body>

<h2>Welcome to the AstrologyClock chart generator</h2>
<p>
  This service can be used to generate a variety of astrological charts.  A PNG is generated.
</p>
<p>
  Options are below:
  <dl>
    <dt>d</dt>
    <dd>
      <strong>diameter</strong> -
      The width and height for the generated graphic.  Minimum is 250, maximum is 800,
      and default is 500.  Keep in mind you can use HTML to further compress or expand the image.
    </dd>
    <dt>t</dt>
    <dd>
      <strong>time</strong> -
      The time and date can be specified in a couple different ways, or left out
      and the current time will be used.
      The year, month, day, hour, minute and second can be entered, separated by spaces or other punctuation.
      If a single number is given it is interpreted as "unix time" (seconds past 1970).
      <ul>
        <li>1974-2-12 16:00</li>
        <li>1974 12 31 15 23</li>
        <li>1321009871</li>
      </ul>
      The timezone can be specified with the "z" parameter.
    </dd>
    <dt>z</dt>
    <dd>
      <strong>timezone</strong> -
      This value is the number of hours east of GMT.  So, -8 is PST, -7 is PDT or MST, 5.5 is IST.
      You can also specify the names of some common time zones, like GMT or America/Los_Angeles.
    </dd>
    <dt>t2</dt>
    <dd>
      <strong>time for outer ring</strong> -
      A second date and time can be given, and an outer ring will be displayed.
      You will probably want to specify "z2" as well.
    </dd>
    <dt>z2</dt>
    <dd>
      <strong>timezone for outer ring</strong> -
      The timezone for the outer ring.
    </dd>
    <dt>lat</dt>
    <dd>
      <strong>latitude</strong> -
      The latitude is specified in degrees north of the equator.
      The default is 40 degrees north but may change in the future.
    </dd>
    <dt>lng</dt>
    <dd>
      <strong>longitude</strong> -
      The longitude is specified in degrees, positive being west and negative being east.
    </dd>
    <dt>h</dt>
    <dd>
      <strong>houses</strong> -
      The display of houses can be turned off with a value of 0.  In the future, house systems will
      be specified with this parameter.
    </dd>
    <dt>center</dt>
    <dd>
      <strong>geocentric/heliocentric</strong> -
      To draw a heliocentric chart, specify "sun".
    </dd>
    <dt>caption</dt>
    <dd>
      <strong>short title</strong> -
      You can specify text to display in the top left corner.  It's better to use CSS for this sort of thing
      but sometimes it's helpful to be able to add a quick label as part of the image.
    </dd>
    <dt>hide</dt>
    <dd>
      <strong>planets to hide</strong> -
      The planets you list will not be displayed.  Planet names should begin with capital letters,
      and be separated with spaces or commas. See "show" for a list.
    </dd>
    <dt>show</dt>
    <dd>
      <strong>planets to show</strong> -
      The planets you list will be displayed.  Planet names should begin with capital letters,
      and be separated with spaces or commas.
      Currently available are:
      Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune, Pluto,
      NorthNode, SouthNode, Chiron, Ceres, Sedna
    </dd>
    <dt>only</dt>
    <dd>
      <strong>only show listed planets</strong> -
      This option disables *all* planets, then enables the ones you list.  This is the easier than trying to disable all
      planets except the ones you are interested in.
    </dd>
    <dt>square</dt>
    <dd>
      <strong>south indian style</strong> -
      A square (South Indian style) chart can be drawn by specifying this option.
    </dd>
    <dt>zod</dt>
    <dd>
      <strong>zodiac</strong> -
      You can specify any of these zodiacs:
      <ul>
        <li>Tropical - the default, equinox-based zodiac</li>
        <li>Fagan-Bradley</li>
        <li>Raman - B.V. Raman</li>
        <li>Lahiri - N.C. Lahiri</li>
      </ul>
    </dd>
    <!--
    <dt>stars</dt>
    <dd>
      <strong>show fixed stars</strong> -
      Displays major stars inside the zodiac belt.  There is no room for labels so they are
      not very useful at the moment.
    </dd>
    -->
  </dl>
</p>

<hr/>

<div class="examples">
  <h2>Examples</h2>

  <strong>New York, right now</strong><br/>
  <img width="350" height="350" src="http://astrology-clock.com/generator/generate?lat=40.8&lng=74&d=350"/>
  <div class="sub">
    <img width="100" height="100" src="http://astrology-clock.com/generator/generate?lat=40.8&lng=74&d=300"/>
    Really small.
    <br/>
    <img width="300" height="300" src="http://astrology-clock.com/generator/generate?lat=40.8&lng=74&d=300&hide=Pluto"/>
    Sorry, Pluto.
    <br/>
  </div>

  <hr/>
  <strong>Seattle, 11/11/2011</strong><br/>
  <img width="350" height="350" src="http://astrology-clock.com/generator/generate?lat=47.5&lng=122.33&d=350&t=2011+11+11+11+11&z=PST"/>
  
  <div class="sub">

      <img width="250" height="250" src="http://astrology-clock.com/generator/generate?lat=47.5&lng=122.33&d=250&t=2011+11+11+11+11&z=PST&only=Neptune+Mars+Moon"/>
      Same as above, highlighting one pattern and smaller.
      <br/>
    
      <img width="150" height="150" src="http://astrology-clock.com/generator/generate?lat=47.5&lng=122.33&d=300&t=2011+11+11+11+11&z=PST&only=Neptune+Mars+Moon"/>
      Even smaller, using browser to compress image.
      <br/>
    
      <img width="300" height="300" src="http://astrology-clock.com/generator/generate?lat=47.5&lng=122.33&d=450&t=2011+11+11+11+11&z=PST&center=sun&h=0"/>
      Heliocentric.
      <br/>
    
      <img width="350" height="350" src="http://astrology-clock.com/generator/generate?lat=47.5&lng=122.33&d=350&t=2011+11+11+11+11&z=PST&square=1&zod=raman&hide=Pluto+Neptune+Uranus&show=NorthNode+SouthNode"/>
      South Indian style.
      <br/>
  </div>

</div>

<script type="text/javascript">
  var imgs = $("img");
  for (var n=0; n < imgs.length; n++)
  {
    var txt = "<br/><em>" + imgs[n].outerHTML.replace("<","&lt;").replace(">","&gt;") + "</em><br/>";
    $(imgs[n]).after( txt );
  }
</script>

</body>
</html>
