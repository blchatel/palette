#pragma version(1)
#pragma rs java_package_name(ch.epfl.cs413.palettev01)

float white_x, white_y, white_z;
float XYZEpsilon, XYZKappa;

//Method to keep the result between 0 and 1
static float bound (float val) {
    float m = fmax(0.0f, val);
    return fmin(1.0f, m);
}

static float PivotRgb(float n) {
	return (n > 0.04045f)? 100.0f*native_powr((n + 0.055f)/1.055f, 2.4f) : 100.0f*n/12.92f;
}

static float PivotXYZ(float n) {
	return (n > XYZEpsilon)? native_powr(n, 1.0f/3.0f) : (XYZKappa*n + 16.0f)/116.0f;
}

/// Convert RGB color to Lab color
static float3 RGB2LAB ( float3 rgb_color)
{
	float r, g, b;
	float x, y, z;

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

/// Convert Lab color to RGB color
static float3 LAB2RGB(float3 lab_color) {
  float L_value = lab_color.r;
  float A_value = lab_color.g;
  float B_value = lab_color.b;
  float y = (L_value + 16.0f)/116.0f;
  float x = A_value/500.0f + y;
  float  z = y - B_value/200.0f;
  float t1, t2, t3;
  float tx, ty, tz;
  float r,g,b;

  float x3 = x * x * x;
  if(x3 > XYZEpsilon){
    t1 = x3;
  } else {
    t1 = (x - 16.0f/116.0f)/7.787f;
  }
  if (L_value > (XYZKappa*XYZEpsilon)){
    float Lt = (L_value + 16.0f)/116.0f;
    t2 = Lt * Lt * Lt;
  } else {
    t2 = L_value/XYZKappa;
  }
  float z3 = z * z * z;
  if (z3 > XYZEpsilon){
    t3 = z3;
  } else {
    t3 = (z - 16.0f/116.0f)/7.787f;
  }
  tx = t1 * white_x / 100.0f;
  ty = t2 * white_y / 100.0f;
  tz = t3 * white_z / 100.0f;

  r = tx*3.2406f + ty*(-1.5372f) + tz*(-0.4986f);
  g = tx*(-0.9689f) + ty*1.8758f + tz*0.0415f;
  b = tx*0.0557f + ty*(-0.2040f) + tz*1.0570f;
  if (r > 0.0031308f){
    r = 1.055f * native_powr(r, 1.0f/2.4f) - 0.055f;
  } else {
    r = 12.92f * r;
  }
  if (g > 0.0031308f) {
    g = 1.055f * native_powr(g, 1.0f/2.4f) - 0.055f;
  } else {
    g = 12.92f * g;
  }
  if (b > 0.0031308f){
    b = 1.055f * native_powr(b, 1.0f / 2.4f) - 0.055f;
  } else {
    b = 12.92f * b;
  }

  float3 res;
  res.r = r;
  res.g = g;
  res.b = b;
  return res;
}

/// Check if the given Lab color is out of boundary
static bool out_boundary(float3 color_lab) {
    float3 color_rgb;
    float delta;
    delta = 0.00001f;
    color_rgb = LAB2RGB(color_lab);
    return ((color_rgb.r < -delta) || (color_rgb.r > 1.0f + delta) ||
             (color_rgb.g < -delta) || (color_rgb.g > 1.0f + delta) ||
             (color_rgb.b < -delta) || (color_rgb.b > 1.0f + delta));
}


static float3 find_out(float3 c0, float3 diff) {
    float3 res, new_diff;
    int i;
    bool out;
    new_diff = diff;
    res = c0;
    for (i = 0; i < 10; i++) {
        out = out_boundary(res);
        new_diff = out? new_diff : new_diff * 2.0f;
        res = out? res : res + new_diff;
    }
    return res;
}

/// Find the boundary point in lab space
static float3 find_boundary(float3 c0, float3 c1) {
    float3 r, l, c;
    int i;
    bool out;
    r = c1;
    l = c0;
    for (i = 0; i < 15; i++) {
        c = (r + l) / 2.0f;
        out = out_boundary(c);
        l = out? l : c;
        r = out? c : r;
    }
    return l;
}

/// Compute the lab distance between 2 colors
static float lab_dis(float3 c0, float3 c1) {
    float3 dc;
    dc = c0 - c1;
    return sqrt(dc.r * dc.r + dc.g * dc.g + dc.b * dc.b);
}

rs_allocation grid;
int grid_g;
int paletteSize;
rs_allocation old_palette, new_palette;
rs_allocation diff;
rs_allocation c_rate;

uchar4 __attribute__((kernel)) image_transfer(uchar4 in, uint32_t x, uint32_t y) {
    float4 f4 = rsUnpackColor8888(in);
    float3 rgb;
    float3 res;
    int g1, gx, gy, gz;
    int i, ix, iy, iz, index;
    float3 new_color;
    rgb.r = f4.r;
    rgb.g = f4.g;
    rgb.b = f4.b;
    rgb *= grid_g;
    g1 = grid_g + 1;
    gx = rgb.r;
    gx = min(max(gx, 0), grid_g - 1);
    gy = rgb.g;
    gy = min(max(gy, 0), grid_g - 1);
    gz = rgb.b;
    gz = min(max(gz, 0), grid_g - 1);
    res.r = 0;
    res.g = 0;
    res.b = 0;
    for (i = 0; i < 8; i++) {
        ix = (i >> 2);
        iy = (i >> 1) & 1;
        iz = i & 1;
        index = (gx + ix) * g1 * g1 + (gy + iy) * g1 + gz;
        new_color = rsGetElementAt_float3(grid, index);
        new_color = LAB2RGB(new_color);
        new_color = fmin(fmax(new_color, 0.0f), 1.0f);
        new_color *= fabs(gx + 1 - ix - rgb.r);
        new_color *= fabs(gy + 1 - iy - rgb.g);
        new_color *= fabs(gz + 1 - iz - rgb.b);
        res += new_color;
    }


    return rsPackColorTo8888(res.r, res.g, res.b, f4.a);
}

float3 __attribute__((kernel)) grid_transfer(float3 in, uint32_t x) {
    float3 res;
    float3 c_l, c_r, c_boundary, c_out, c_res, c_diff, c_new, c_c;
    int i, j;
    float rate, d_now, d_target;
    bool out;

    res.r = 0;
    res.g = 0;
    res.b = 0;

    // TODO: take all colors into account
    for (i = 0; i < 1; i++) {
        c_diff = rsGetElementAt_float3(diff, i);
        rate = rsGetElementAt_float(c_rate, i);
        c_new = rsGetElementAt_float3(new_palette, i);
        c_out = c_diff + in;
        c_r = find_out(in, c_diff);
        out = out_boundary(c_out);
        c_r = out? c_out : c_r;
        c_l = out? c_new : in;
        c_boundary = find_boundary(c_l, c_r);
        d_target = lab_dis(in, c_boundary);
        d_target *= rate;
        c_r = c_boundary;
        c_l = in;
        for (j = 0; j < 5; j++) {
            c_c = (c_l + c_r) / 2.0f;
            d_now = lab_dis(in, c_c);
            c_l = (d_now > d_target) ? c_l : c_c;
            c_r = (d_now > d_target) ? c_c : c_r;
        }
        c_res = (rate < 0.000001f) ? in : c_l;
        res += c_res;
    }

    return res;
}

void cal_palette_rate() {
    int i;
    float3 c_old, c_new, c_diff, c_boundary, c_out;
    float rate, d0, d1;
    for (i = 0; i < paletteSize; i++) {
        c_old = rsGetElementAt_float3(old_palette, i);
        c_new = rsGetElementAt_float3(new_palette, i);
        c_diff = c_new - c_old;
        c_out = find_out(c_old, c_diff);
        c_boundary = find_boundary(c_old, c_out);
        d0 = lab_dis(c_old, c_new);
        d1 = lab_dis(c_old, c_boundary);
        rate = (d0 < 0.000001f) ? 0.0f : d0 / d1;

        rsSetElementAt_float3(diff, c_diff, i);
        rsSetElementAt_float(c_rate, rate, i);
    }
}

/// RGB grid init
void initGrid2() {
    int i;
    float3 res;
    for (i = 0; i < 10; i++) {
        res.r = 0.1f * i;
        res.g = 0.1f * i;
        res.b = 0.1f * i;
        rsSetElementAt_float3(grid, res, i);
    }
}

/// Lab grid init
void initGrid() {
    int g1 = grid_g + 1;
    float3 c;
    float3 lab_c;
    int i, j, k, index;
    for (i = 0; i < g1; i++)
        for (j = 0; j < g1; j++)
            for (k = 0; k < g1; k++) {
                index = i * g1 * g1 + j * g1 + k;
                c.r = 1.0f * i / grid_g;
                c.g = 1.0f * j / grid_g;
                c.b = 1.0f * k / grid_g;
                lab_c = RGB2LAB(c);
                rsSetElementAt_float3(grid, lab_c, index);
            }
}

uchar4 __attribute__((kernel)) test(uchar4 in, uint32_t x, uint32_t y) {
    float4 f4 = rsUnpackColor8888(in);
    float3 rgb;
    rgb.r = f4.r;
    rgb.g = f4.g;
    rgb.b = f4.b;
    float3 res;
    res = RGB2LAB(rgb);
    res /= 100.f;
    // res = LAB2RGB(res);
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
	white_x = 95.047f;
	white_y = 100.000f;
	white_z = 108.883f;
	XYZEpsilon = 216.0f/24389.0f;
	XYZKappa = 24389.0f/27.0f;
}

/// -- Tests --
int i;

float3 __attribute__((kernel)) grid_transfer_i(float3 in, uint32_t x) {
    float3 res;
    float3 c_l, c_r, c_boundary, c_out, c_res, c_diff, c_new, c_c;
    int j;
    float rate, d_now, d_target;
    bool out;

    res.r = 0;
    res.g = 0;
    res.b = 0;

    c_diff = rsGetElementAt_float3(diff, i);
    rate = rsGetElementAt_float(c_rate, i);
    c_new = rsGetElementAt_float3(new_palette, i);
    c_out = c_diff + in;
    c_r = find_out(in, c_diff);
    out = out_boundary(c_out);
    c_r = out? c_out : c_r;
    c_l = out? c_new : in;
    c_boundary = find_boundary(c_l, c_r);
    d_target = lab_dis(in, c_boundary);
    d_target *= rate;
    c_r = c_boundary;
    c_l = in;
    for (j = 0; j < 5; j++) {
        c_c = (c_l + c_r) / 2.0f;
        d_now = lab_dis(in, c_c);
        c_l = (d_now > d_target) ? c_l : c_c;
        c_r = (d_now > d_target) ? c_c : c_r;
    }
    c_res = (rate < 0.000001f) ? in : c_l;
    res += c_res;

    return res;
}