function [ smooth ] = smoothL( x, d )
%SMOOTHL Summary of this function goes here
%   Detailed explanation goes here
    lambda = 0.2 * log(2);
    
    smooth = log(exp(lambda*x) + exp(lambda*d) - 1) / lambda - x;

end

