function [ color_l ] = findNewColor( color_original, color_boundary, C_rate)
%findBoundary Find the Cb point 
%   Detailed explanation goes here
color_l = color_original;
color_r = color_boundary;
target_dis = C_rate * labDistance(color_l, color_r);

for it = 1:7
    middle = (color_l + color_r) / 2;
    dis = labDistance(color_original, middle);
    to_right = dis > target_dis;
    to_left = ~to_right;
    color_l(to_left, :) = middle(to_left, :);
    color_r(to_right, :) = middle(to_right, :);
end

end
