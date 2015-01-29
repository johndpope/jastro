<%

%>
<html>
 <head>
  <style type="text/css">
    form {
      margin-left: 24px;
    }
    .date {
      padding-top: 2px;
    }
    .field {
      margin-top: -2px;
      vertical-align: top;
      display: inline-block;
      text-align: center;
    }
    .hint {
      color: #808080;
      font-size: 11px;
      font-style: italic;
    }
    .error {
      background-color: #ffc0c0;
    }
    .empty {
      background-color: #ffffc0;
    }
  </style>
  <script type="text/javascript" src="../jquery.js"></script>
  <script type="text/javascript">
    $(function(){
      var overall = 0;
      function tweak( field, valid, bit )
      {
        if (valid)
          field.removeClass( "error" ).removeClass( "empty" );
        else if (field.val() == "")
          field.addClass( "empty" ).removeClass( "error" );
        else
          field.addClass( "error" ).removeClass( "empty" );
        overall = (overall & ~(1<<bit));
        if (! valid)
          overall |= (1<<bit);
      }
      function validateNumeric( fieldName, lo, hi, bit )
      {
        var field = $("input[name=" + fieldName + "]");
        function adjust( evt )
        {
          var val = field.val()
          var valid = ! (val == ""  ||  isNaN( val ));
          if (valid)
            valid = (val >= lo  &&  val <= hi);
          tweak( field, valid, bit );
        }
        field.click( adjust );
        field.keyup( adjust );
        field.change( adjust );
        adjust();
      }
      function validateZone( fieldName, bit )
      {
        var field = $("input[name=" + fieldName + "]");
        function adjust( evt )
        {
          var val = field.val().toLowerCase();
          var valid = val.match( /^([a-z]{2,}t|gmt[\+\-]\d+)$/ );
          tweak( field, valid, bit );
        }
        field.click( adjust );
        field.keyup( adjust );
        field.change( adjust );
        adjust();
      }
      function v( fieldName )
      {
        return $("input[name=" + fieldName + "]").val();
      }

      validateNumeric( "year", 1900, 2100, 0 );
      validateNumeric( "month", 1, 12, 1 );
      validateNumeric( "day", 1, 31, 2 );
      validateNumeric( "hour", 0, 23, 3 );
      validateNumeric( "minute", 0, 59, 4 );
      validateZone( "zone", 5 );

      $("#calculate").click( function() {
        if (overall != 0)
        {
          alert( "Please fill out the fields completely." );
          return;
        }
        var timeStr = v("year") + "/" + v("month") + "/" + v("day") + " " + v("hour") + ":" + v("minute") + " " + v("zone");
        $(".data").load( "data.jsp?time=" + escape(timeStr) );
      } );
    });
  </script>
 </head>
 <body>
  <h1>Vimsottari Dasa Calculation</h1>
  <form>
   <div class='date'>
    <div class='field'>
      <input name='year' type='text' size='4'/>
      <div class='hint'>year</div>
    </div>
    /
    <div class='field'>
     <input name='month' type='text' size='2'/>
     <div class='hint'>month</div>
    </div>
    /
    <div class='field'>
     <input name='day' type='text' size='2'/>
     <div class='hint'>day</div>
    </div>
    &#160;
    <div class='field'>
     <input name='hour' type='text' size='2'/>
     <div class='hint'>hour</div>
    </div>
    :
    <div class='field'>
     <input name='minute' type='text' size='2'/>
     <div class='hint'>minute</div>
    </div>
    &#160;
    <div class='field'>
     <input name='zone' type='text' size='4'/>
     <div class='hint'>timezone</div>
    </div>
   </div>
   <input type='button' id='calculate' value="Calculate"/>
  </form>
  <div class='data'></div>
 </body>
</html>