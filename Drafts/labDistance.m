%% Function that compute the lab distance between 2 colors
function [ distance ] = labDistance( c1, c2 )
    l1 = c1(:, 1);
    a1 = c1(:, 2);
    b1 = c1(:, 3);
    l2 = c2(:, 1);
    a2 = c2(:, 2);
    b2 = c2(:, 3);
    
    K1 = 0.045;
    K2 = 0.015;
	del_L = l1 - l2;
	c1 = sqrt(a1 .* a1 + b1 .* b1); 
	c2 = sqrt(a2 .* a2 + b2 .* b2);
	c_ab = c1 - c2;
	h_ab = (a1 - a2) .* (a1 - a2) + (b1 - b2) .* (b1 - b2) - c_ab .* c_ab;
	distance = sqrt(del_L .* del_L + c_ab .* c_ab ./ (1 + K1 .* c1) ./ (1 + K1 .* c1) + h_ab ./ (1 + K2 .* c1) ./ (1 + K2 .* c1));
end

