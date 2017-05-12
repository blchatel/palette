#pragma version(1)
#pragma rs java_package_name(ch.epfl.cs413.palettev01)

typedef struct Bins Bins;
struct Bins {
    float3 meanColor;
    int count;
};


float white_x, white_y, white_z;
float XYZEpsilon, XYZKappa;

static float PivotRgb(float n) {
	return (n > 0.04045f)? 100.0f*powr((n + 0.055f)/1.055f, 2.4f) : 100.0f*n/12.92f;
}

static float PivotXYZ(float n) {
	return (n > XYZEpsilon)? powr(n, 1.0f/3.0f) : (XYZKappa*n + 16.0f)/116.0f;
}

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

/// The maximum number of iterations if there is no convergence
int max_it = 50;
/// The number of bins
const int b = 16;
/// To define the range of each bin
const int inv_b = 16;

/// The bins array
static Bins bins[b][b][b];


/// This describe the number of color in the palette
const int K = 6;

void __attribute__((kernel)) assign_bins(uchar4 in) {
    float4 argb = rsUnpackColor8888(in);
    float3 rgb;
    rgb.r = argb.r;
    rgb.g = argb.g;
    rgb.b = argb.b;

    float3 lab = RGB2LAB(rgb);

    int x = rgb.r / b;
    int y = rgb.g / b;
    int z = rgb.b / b;
    Bins bin = bins[x][y][z];
    bin.count++;
    bin.meanColor += lab;
    bins[x][y][z] = bin;
}

void normalize_bins () {
    for (int x; x < b; x++) {
    for (int y; y < b; y++) {
    for (int z; z < b; z++) {
        Bins bin = bins[x][y][z];
        if (bin.count > 0) {
            bin.meanColor /= bin.count;
            bins[x][y][z] = bin;
        }
    }
    }
    }
}

static float lab_square_dist(float3 lab1, float3 lab2) {
    return (lab1[0]-lab2[0])*(lab1[0]-lab2[0]) + (lab1[1]-lab2[1])*(lab1[1]-lab2[1]) + (lab1[2]-lab2[2])*(lab1[2]-lab2[2]);
}

void init() {
    white_x = 95.047f;
	white_y = 100.000f;
	white_z = 108.883f;
	XYZEpsilon = 216.0f/24389.0f;
	XYZKappa = 24389.0f/27.0f;
}