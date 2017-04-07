#pragma version(1)
#pragma rs java_package_name(ch.epfl.cs413.palettev01)


float3 old_Plab, new_Plab, Cb;
float c_rate;


//Method to keep the result between 0 and 1
static float bound (float val) {
    float m = fmax(0.0f, val);
    return fmin(1.0f, m);
}

static float PivotRgb(float n) {
	return (n > 0.04045f)? 100.0f*native_powr((n + 0.055f)/1.055f, 2.4f) : 100.0f*n/12.92f;
}

static float PivotXYZ(float n) {
    float XYZEpsilon, XYZKappa;
	XYZEpsilon = 216.0f/24389.0f;
	XYZKappa = 24389.0f/27.0f;
	return (n > XYZEpsilon)? native_powr(n, 1.0f/3.0f) : (XYZKappa*n + 16)/116;
}

static float3 RGB2LAB ( float3 rgb_color)
{
	float r, g, b;
	float x, y, z;
    float white_x, white_y, white_z;
	white_x = 95.047f;
	white_y = 100.000f;
	white_z = 108.883f;

	r = PivotRgb(rgb_color.r);
	g = PivotRgb(rgb_color.g);
	b = PivotRgb(rgb_color.b);

	x = r*0.4124f + g*0.3576f + b*0.1805f;
	y = r*0.2126f + g*0.7152f + b*0.0722f;
	z = r*0.0193f + g*0.1192f + b*0.9505f;

    float3 res;
	res.r = 116.0f * PivotXYZ( y / white_y ) - 16.0f;
	res.g = 500.0f * ( PivotXYZ( x / white_x ) - PivotXYZ( y / white_y ) );
	res.b = 200.0f * ( PivotXYZ( y / white_y ) - PivotXYZ( z / white_z ) );

	return res;
}

static float3 LAB2RGB(float3 lab_color) {
  float L_value = lab_color.r;
  float A_value = lab_color.g;
  float B_value = lab_color.b;
  float y = (L_value + 16.0)/116.0;
  float x = A_value/500.0 + y;
  float  z = y - B_value/200.0;
  float t1, t2, t3;
  float tx, ty, tz;
  float r,g,b;
  float white_x, white_y, white_z;
  white_x = 95.047f;
  white_y = 100.000f;
  white_z = 108.883f;
    float XYZEpsilon, XYZKappa;
	XYZEpsilon = 216.0f/24389.0f;
	XYZKappa = 24389.0f/27.0f;

  float x3 = x * x * x;
  if(x3 > XYZEpsilon){
    t1 = x3;
  } else {
    t1 = (x - 16.0/116.0)/7.787;
  }
  if (L_value > (XYZKappa*XYZEpsilon)){
    float Lt = (L_value + 16.0)/116.0;
    t2 = Lt * Lt * Lt;
  } else {
    t2 = L_value/XYZKappa;
  }
  float z3 = z * z * z;
  if (z3 > XYZEpsilon){
    t3 = z3;
  } else {
    t3 = (z - 16.0/116.0)/7.787;
  }
  tx = t1 * white_x / 100.0f;
  ty = t2 * white_y / 100.0f;
  tz = t3 * white_z / 100.0f;

  r = tx*3.2406 + ty*(-1.5372) + tz*(-0.4986);
  g = tx*(-0.9689) + ty*1.8758 + tz*0.0415;
  b = tx*0.0557 + ty*(-0.2040) + tz*1.0570;
  if (r > 0.0031308){
    r = 1.055*native_powr(r, 1.0f/2.4) - 0.055;
  } else {
    r = 12.92*r;
  }
  if (g > 0.0031308) {
    g = 1.055*native_powr(g, 1.0f/2.4) - 0.055;
  } else {
    g = 12.92*g;
  }
  if (b > 0.0031308){
    b = 1.055*native_powr(b, 1.0f/2.4) - 0.055;
  } else {
    b = 12.92*b;
  }

  float3 res;
  res.r = r;
  res.g = g;
  res.b = b;
  return res;
}

uchar4 __attribute__((kernel)) test(uchar4 in, uint32_t x, uint32_t y) {
    float4 f4 = rsUnpackColor8888(in);
    float3 rgb;
    rgb.r = f4.r;
    rgb.g = f4.g;
    rgb.b = f4.b;
    float3 res;
    res = RGB2LAB(rgb);
    res.r /= 100.0f;
    res.g /= 100.0f;
    res.b /= 100.0f;
    //res = LAB2RGB(res);
    return rsPackColorTo8888(res.r, res.g, res.b, f4.a);
}

uchar4 __attribute__((kernel)) blackWhite(uchar4 in, uint32_t x, uint32_t y) {
    float4 f4 = rsUnpackColor8888(in);
    //if (pown(f4.r, 2.0f) + pown(f4.g, 2.0f) + pown(f4.b, 2.0f) > 1.8f)
    if (f4.r > 0.13f)
        return rsPackColorTo8888(1.0f, 1.0f, 1.0f, f4.a);
    else
        return rsPackColorTo8888(0.0f, 0.0f, 0.0f, f4.a);
}

void init() {
}

void init_fun() {

	old_Plab.r = 73.2794f;
	old_Plab.g = -0.6067f;
	old_Plab.b = -19.2662f;
	new_Plab.r = 58.3956f;
	new_Plab.g = 19.7365;
	new_Plab.b = 7.8631;
}
