$fn = 80;


module polygon_rotator(points_diameter,points_position) {
    // Create 2D profile of half the bore
    number_of_points = len(points_position)-1;
    profile_points = [
        for(i = [0:number_of_points]) 
            [points_diameter[i]/2, points_position[i]],
        for(i = [number_of_points:-1:0]) [0, points_position[i]]
    ];
    
    // Rotate extrude to create 3D bore
    rotate_extrude($fn = 64) {
        //linear_extrude(1){
            polygon(profile_points);
        //}
        
    }
}

module finger_holes(finger_holes_diameter,finger_holes_position){
    for(i = [0:len(finger_holes_diameter)-1]){
        translate([0,0,finger_holes_position[i]])
        rotate([0,90,0])
        linear_extrude(40){
        
        circle(r = finger_holes_diameter[i]/2);
        }
    }    
}




    

// Render the bore
//
module rudall_and_carte(
    outside_diameters,outside_positions,
    bore_diameter,bore_positions,
    finger_holes_diameter,finger_holes_position
){
    difference(){
        polygon_rotator(outside_diameters,outside_positions);
        polygon_rotator(bore_diameter,bore_positions);
        finger_holes(finger_holes_diameter,finger_holes_position);
    }
}

// Example of using module
// Flute Bore Profile
inside_lengths = [-0.01, 3, 4, 6, 8, 11, 13, 17, 29, 35, 42, 47, 51, 60, 65, 72, 79, 84, 96, 102, 107, 112, 117, 123, 130, 139, 145, 151, 155, 161, 165, 173, 177, 183, 187, 188, 193, 200, 205, 207, 209.01];

lengths = [0, 3, 4, 6, 8, 11, 13, 17, 29, 35, 42, 47, 51, 60, 65, 72, 79, 84, 96, 102, 107, 112, 117, 123, 130, 139, 145, 151, 155, 161, 165, 173, 177, 183, 187, 188, 193, 200, 205, 207, 209];


diameters = [18.9, 18.8, 18.7, 18.6, 18.5, 18.4, 18.3, 18.2, 18.1, 18.0, 17.9, 17.8, 17.7, 17.6, 17.5, 17.4, 17.3, 17.2, 17.1, 17.0, 16.9, 16.8, 16.7, 16.6, 16.5, 16.4, 16.3, 16.2, 16.1, 16.0, 15.9, 15.8, 15.7, 15.6, 15.5, 15.4, 15.3, 15.2, 15.3, 15.4, 15.5];

outside_diameters = [30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,
30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30,30];

finger_holes_diameter = [10,10,10,10];
finger_holes_position = [30,60,90,120];

rudall_and_carte(
    outside_diameters,lengths,
    diameters,inside_lengths,
    finger_holes_diameter,finger_holes_position
);