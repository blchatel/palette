function [ grid_new_l ] = findBoundary2( grid_new_l, grid_new_r)
%findBoundary Find the Cb point 
%   Detailed explanation goes here
for it = 1:7
    middle = (grid_new_l + grid_new_r) / 2;
    to_right = outBoundaryArray(middle);
    to_left = ~to_right;
    grid_new_l(to_left, :) = middle(to_left, :);
    grid_new_r(to_right, :) = middle(to_right, :);
end

end

