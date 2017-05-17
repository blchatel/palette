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
	return (n > 0.04045f)? 100.0f*powr((n + 0.055f)/1.055f, 2.4f) : 100.0f*n/12.92f;
}

static float PivotXYZ(float n) {
	return (n > XYZEpsilon)? powr(n, 1.0f/3.0f) : (XYZKappa*n + 16.0f)/116.0f;
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
    r = 1.055f * powr(r, 1.0f/2.4f) - 0.055f;
  } else {
    r = 12.92f * r;
  }
  if (g > 0.0031308f) {
    g = 1.055f * powr(g, 1.0f/2.4f) - 0.055f;
  } else {
    g = 12.92f * g;
  }
  if (b > 0.0031308f){
    b = 1.055f * powr(b, 1.0f / 2.4f) - 0.055f;
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
    float3 res;
    res = diff * 300.0f;
    res += c0;
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
    float K1, K2, cc0, cc1, cab, hab;
    float dis;
    dc = c0 - c1;
    K1 = 0.045f;
    K2 = 0.015f;
    cc0 = sqrt(c0.g * c0.g + c0.b * c0.b);
    cc1 = sqrt(c1.g * c1.g + c1.b * c1.b);
    cab = cc0 - cc1;
    hab = dc.g * dc.g + dc.b * dc.b - cab * cab;
    dis = dc.r * dc.r;
    dis += cab * cab / (1.0f + K1 * cc0) / (1.0f + K1 * cc0);
    dis += hab / (1.0f + K2 * cc0) / (1.0f + K2 * cc0);
    dis = sqrt(dis);

    return dis;
}

static float lab_dis2(float3 c0, float3 c1) {
    float res;
    float3 cd;
    cd = c1 - c0;
    res = cd.r * cd.r + cd.g * cd.g + cd.b * cd.b;
    res = sqrt(res);
    return res;
}

rs_allocation grid;
int grid_g;
int paletteSize;
rs_allocation old_palette, new_palette;
rs_allocation diff;
rs_allocation ccb_l;
rs_allocation cc_l;
rs_allocation palette_distance;
rs_allocation palette_weights;
int RBF_param_coff;

void calculate_distance(int palette_size) {
    int i, j;
    float3 c0, c1;
    float dis;
    float palette_mean_distance = 0;
    for (i = 0; i < palette_size; i++)
        for (j = 0; j < palette_size; j++){
            c0 = rsGetElementAt_float3(old_palette, i);
            c1 = rsGetElementAt_float3(old_palette, j);
            dis = lab_dis(c0, c1);
            rsSetElementAt_float(palette_distance, dis, i * palette_size + j);
            palette_mean_distance += dis;
        }
    palette_mean_distance /= 1.0f * palette_size * palette_size;
    rsSetElementAt_float(palette_distance, palette_mean_distance, palette_size * palette_size);
    for (i = 0; i < palette_size; i++)
        for (j = 0; j < palette_size; j++){
            dis = rsGetElementAt_float(palette_distance, i * palette_size + j);
            dis = exp( - dis * dis * RBF_param_coff / palette_mean_distance / palette_mean_distance);
            rsSetElementAt_float(palette_distance, dis, i * palette_size + j);
        }
}

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
        index = (gx + ix) * g1 * g1 + (gy + iy) * g1 + gz + iz;
        new_color = rsGetElementAt_float3(grid, index);
        // new_color = LAB2RGB(new_color);
        new_color = fmin(fmax(new_color, 0.0f), 1.0f);
        new_color *= fabs(1.0f + gx - ix - rgb.r);
        new_color *= fabs(1.0f + gy - iy - rgb.g);
        new_color *= fabs(1.0f + gz - iz - rgb.b);
        res += new_color;
    }


    return rsPackColorTo8888(res.r, res.g, res.b, f4.a);
}

float3 __attribute__((kernel)) grid_transfer(float3 in, uint32_t x) {
    float3 in_lab, res, res_rgb;
    float3 c_l, c_r, c_boundary, c_out, c_res, c_diff, c_new, c_c;
    int i, j;
    float c_c_l, d_now, d_target, c_cb_l, tmp0;
    bool out;
    float weight_sum, weight, dis, w;
    float3 c_old;
    float mean_dis;

    in_lab = RGB2LAB(in);
    res = in_lab;

    weight_sum = 0.0f;
    mean_dis = rsGetElementAt_float(palette_weights, paletteSize * paletteSize);
    for (i = 0; i < paletteSize; i++) {
        weight = 0.0f;
        for (j = 0; j < paletteSize; j++) {
            c_old = rsGetElementAt_float3(old_palette, j);
            w = rsGetElementAt_float(palette_weights, i * paletteSize + j);
            dis = lab_dis(in_lab, c_old);
            weight += w * exp( - dis * dis * RBF_param_coff / mean_dis / mean_dis);
        }
        if (weight > 0.0f)
            weight_sum += weight;
    }

    // TODO: take all colors into account
    for (i = 0; i < paletteSize; i++) {
        weight = 0.0f;
        for (j = 0; j < paletteSize; j++) {
            c_old = rsGetElementAt_float3(old_palette, j);
            w = rsGetElementAt_float(palette_weights, i * paletteSize + j);
            dis = lab_dis(in_lab, c_old);
            weight += w * exp( - dis * dis * RBF_param_coff / mean_dis / mean_dis);
        }
        if (weight > 0.0f)
            weight /= weight_sum;
        else
            weight = 0.0f;

        c_diff = rsGetElementAt_float3(diff, i);
        c_cb_l = rsGetElementAt_float(ccb_l, i);
        c_c_l = rsGetElementAt_float(cc_l, i);
        c_new = rsGetElementAt_float3(new_palette, i);
        c_out = c_diff + in_lab;
        c_r = find_out(in_lab, c_diff);
        out = out_boundary(c_out);
        c_r = out? c_out : c_r;
        c_l = out? c_new : in;
        c_boundary = find_boundary(c_l, c_r);

        tmp0 = lab_dis2(in_lab, c_boundary);

        d_target = tmp0;
        d_target = c_c_l < 0.00001f? d_target: d_target / c_cb_l;
        d_target = fmin(d_target, 1.0f);
        d_target *= c_c_l;
        d_target = c_c_l < 0.00001f? 0.0f: d_target / tmp0;

        c_res = c_boundary - in_lab;
        c_res.r = out? c_res.r: c_diff.r;
        c_res.g *= d_target;
        c_res.b *= d_target;

        if (weight > 0.0f)
            res += c_res * weight;
    }

    res_rgb = LAB2RGB(res);
    res_rgb = fmin(fmax(res_rgb, 0.0f), 1.0f);
    return res_rgb;
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
        d0 = lab_dis2(c_old, c_new);
        d1 = lab_dis2(c_old, c_boundary);
        d0 = (d0 < 0.000001f) ? 0.0f : d0;

        rsSetElementAt_float3(diff, c_diff, i);
        rsSetElementAt_float(ccb_l, rate, i);
        rsSetElementAt_float(cc_l, d0, i);
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
                // lab_c = RGB2LAB(c);
                rsSetElementAt_float3(grid, c, index);
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
	RBF_param_coff = 5.0f;
}

float3 __attribute__((kernel)) image_to_lab(int in) {
    float3 res;
    float3 rgb;
    rgb.r = (in >> 16) & 0xff;
    rgb.g = (in >> 8) & 0xff;
    rgb.b = in & 0xff;
    rgb /= 256.0f;
    res = RGB2LAB(rgb);
    return res;
}

int bin_b;
int3 __attribute__((kernel)) image_to_binIndex(int in) {
    int3 res;
    int r,g,b;
    int inv_b;
    r = (in >> 16) & 0xff;
    g = (in >> 8) & 0xff;
    b = in & 0xff;
    res.r = r * bin_b / 256;
    res.g = g * bin_b / 256;
    res.b = b * bin_b / 256;
    res = max(min(res, bin_b), 0);
    return res;
}

int image_size;
void image_to_bins(rs_allocation image_lab, rs_allocation image_binIndex,
                    rs_allocation bin_lab, rs_allocation bin_num) {
    int b3, i, index;
    int bNum;
    float3 bLab;
    float3 iLab;
    int3 iBin;
    b3 = bin_b * bin_b * bin_b;
    bLab.r = 0.0f;
    bLab.g = 0.0f;
    bLab.b = 0.0f;
    for (i = 0; i < b3; i++) {
        rsSetElementAt_float3(bin_lab, bLab, i);
        rsSetElementAt_int(bin_num, 0, i);
    }
    for (i = 0; i < image_size; i++) {
        iLab = rsGetElementAt_float3(image_lab, i);
        iBin = rsGetElementAt_int3(image_binIndex, i);
        index = iBin.r * bin_b * bin_b + iBin.g * bin_b + iBin.b;
        bLab = rsGetElementAt_float3(bin_lab, index);
        bNum = rsGetElementAt_int(bin_num, index);
        bNum += 1;
        bLab += iLab;
        rsSetElementAt_float3(bin_lab, bLab, index);
        rsSetElementAt_int(bin_num, bNum, index);
    }
}